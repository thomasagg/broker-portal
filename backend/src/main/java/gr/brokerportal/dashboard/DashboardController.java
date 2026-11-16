package gr.brokerportal.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.brokerportal.auth.CurrentUser;
import gr.brokerportal.submission.SubmissionRepository;
import gr.brokerportal.submission.SubmissionRepository.StatusCount;
import gr.brokerportal.submission.SubmissionResponse;
import gr.brokerportal.submission.SubmissionService;
import gr.brokerportal.submission.SubmissionStatus;
import gr.brokerportal.user.User;

@RestController
class DashboardController {

    private static final Duration TURNAROUND_WINDOW = Duration.ofDays(90);
    private static final int EXPIRY_WARNING_DAYS = 7;

    private final SubmissionService service;
    private final SubmissionRepository submissions;
    private final CurrentUser currentUser;
    private final Clock clock;

    DashboardController(SubmissionService service, SubmissionRepository submissions, CurrentUser currentUser,
            Clock clock) {
        this.service = service;
        this.submissions = submissions;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    record Turnaround(Double submittedToQuotedHours, long quotedSample, Double submittedToBoundHours,
            long boundSample) {
    }

    record Dashboard(Map<SubmissionStatus, Long> countsByStatus, List<SubmissionResponse> waitingOnMe,
            List<SubmissionResponse> expiringQuotes, Turnaround turnaround) {
    }

    @GetMapping("/api/dashboard")
    @Transactional(readOnly = true)
    Dashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        User me = currentUser.from(jwt);

        List<StatusCount> rows = me.isBroker()
                ? submissions.countByStatusForBrokerage(me.getBrokerage().getId())
                : submissions.countByStatus();
        var counts = new EnumMap<SubmissionStatus, Long>(SubmissionStatus.class);
        rows.forEach(r -> counts.put(r.getStatus(), r.getCount()));

        // TODO: fine for now, move to a query once the open pipeline gets into the thousands
        var open = EnumSet.complementOf(EnumSet.of(SubmissionStatus.DECLINED, SubmissionStatus.NOT_TAKEN_UP,
                SubmissionStatus.WITHDRAWN, SubmissionStatus.CANCELLED, SubmissionStatus.BOOKED));
        List<SubmissionResponse> pipeline = service.list(me, open, null);

        List<SubmissionResponse> waitingOnMe = pipeline.stream()
                .filter(s -> s.availableActions().stream().anyMatch(a -> !a.isEscapeHatch()))
                // oldest first: whatever has waited longest goes on top
                .sorted((a, b) -> a.statusChangedAt().compareTo(b.statusChangedAt()))
                .toList();

        LocalDate warnUntil = LocalDate.now(clock).plusDays(EXPIRY_WARNING_DAYS);
        List<SubmissionResponse> expiring = pipeline.stream()
                .filter(s -> s.status() == SubmissionStatus.QUOTED && !s.quoteValidUntil().isAfter(warnUntil))
                .sorted((a, b) -> a.quoteValidUntil().compareTo(b.quoteValidUntil()))
                .toList();

        Instant since = clock.instant().minus(TURNAROUND_WINDOW);
        var quoted = submissions.turnaroundSince(SubmissionStatus.QUOTED.name(), since);
        var bound = submissions.turnaroundSince(SubmissionStatus.BOUND.name(), since);

        return new Dashboard(counts, waitingOnMe, expiring,
                new Turnaround(quoted.getHours(), quoted.getSample(), bound.getHours(), bound.getSample()));
    }
}
