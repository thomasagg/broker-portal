package gr.brokerportal.submission;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gr.brokerportal.user.User;

@Service
public class SubmissionService {

    private final SubmissionRepository submissions;
    private final SubmissionEventRepository events;
    private final SubmissionMessageRepository messages;
    private final Clock clock;

    SubmissionService(SubmissionRepository submissions, SubmissionEventRepository events,
            SubmissionMessageRepository messages, Clock clock) {
        this.submissions = submissions;
        this.events = events;
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> list(User viewer, Collection<SubmissionStatus> statuses, String query) {
        Specification<Submission> spec = visibleTo(viewer);
        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("status").in(statuses));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("insuredName")), like),
                    cb.like(cb.lower(root.get("reference")), like)));
        }
        Instant now = clock.instant();
        return submissions.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(s -> SubmissionResponse.of(s, viewer, now))
                .toList();
    }

    private static Specification<Submission> visibleTo(User viewer) {
        if (viewer.isBroker()) {
            UUID brokerageId = viewer.getBrokerage().getId();
            return (root, q, cb) -> cb.equal(root.get("brokerage").get("id"), brokerageId);
        }
        return (root, q, cb) -> cb.notEqual(root.get("status"), SubmissionStatus.DRAFT);
    }

    @Transactional(readOnly = true)
    public SubmissionDetail get(User viewer, UUID id) {
        return detail(load(viewer, id), viewer);
    }

    @Transactional
    public SubmissionDetail create(User broker, SubmissionDetails details) {
        Instant now = clock.instant();
        String reference = "SUB-%d-%06d".formatted(LocalDate.now(clock).getYear(), submissions.nextReferenceNumber());
        Submission submission = submissions.save(Submission.draft(broker, reference, details, now));
        events.save(new SubmissionEvent(submission, null, SubmissionStatus.DRAFT, broker, null, now));
        return detail(submission, broker);
    }

    @Transactional
    public SubmissionDetail update(User broker, UUID id, SubmissionDetails details) {
        Submission submission = load(broker, id);
        if (!broker.isBroker()) {
            throw new ActionNotAllowedException("Only the broker can change the submission");
        }
        submission.update(details, clock.instant());
        return detail(submission, broker);
    }

    // renewals and similar risks start from last year's answers instead of an empty form
    @Transactional
    public SubmissionDetail copy(User broker, UUID id) {
        SubmissionDetails old = load(broker, id).details();
        SubmissionDetails renewal = new SubmissionDetails(old.lineOfBusiness(), old.insuredName(),
                old.insuredCountry(), old.insuredIndustry(), old.annualRevenue(), old.sumInsured(), old.currency(),
                old.coverageStart() == null ? null : old.coverageStart().plusYears(1),
                old.coverageEnd() == null ? null : old.coverageEnd().plusYears(1), old.targetPremium(),
                old.riskDescription());
        return create(broker, renewal);
    }

    @Transactional
    public SubmissionDetail perform(User actor, UUID id, ActionRequest request) {
        Submission submission = load(actor, id);
        events.save(submission.perform(actor, request, LocalDate.now(clock), clock.instant()));
        return detail(submission, actor);
    }

    @Transactional
    public SubmissionDetail addMessage(User author, UUID id, String body) {
        Submission submission = load(author, id);
        messages.save(new SubmissionMessage(submission, author, body, clock.instant()));
        return detail(submission, author);
    }

    @Transactional
    public int expireQuotes() {
        LocalDate today = LocalDate.now(clock);
        List<Submission> expired = submissions.findByStatusAndQuoteValidUntilBefore(SubmissionStatus.QUOTED, today);
        Instant now = clock.instant();
        expired.forEach(s -> events.save(s.expireQuote(today, now)));
        return expired.size();
    }

    private Submission load(User viewer, UUID id) {
        return submissions.findById(id)
                .filter(s -> s.isVisibleTo(viewer))
                .orElseThrow(SubmissionNotFoundException::new);
    }

    private SubmissionDetail detail(Submission submission, User viewer) {
        return new SubmissionDetail(SubmissionResponse.of(submission, viewer, clock.instant()),
                events.findBySubmissionIdOrderByCreatedAt(submission.getId()).stream()
                        .map(SubmissionDetail.Event::of).toList(),
                messages.findBySubmissionIdOrderByCreatedAt(submission.getId()).stream()
                        .map(SubmissionDetail.Message::of).toList());
    }
}
