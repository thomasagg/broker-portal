package gr.brokerportal.submission;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import gr.brokerportal.user.Brokerage;
import gr.brokerportal.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

@Entity
public class Submission {

    @Id
    private UUID id;

    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    private Brokerage brokerage;

    @ManyToOne(fetch = FetchType.LAZY)
    private User broker;

    @ManyToOne(fetch = FetchType.LAZY)
    private User underwriter;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Enumerated(EnumType.STRING)
    private LineOfBusiness lineOfBusiness;

    private String insuredName;
    private String insuredCountry;
    private String insuredIndustry;
    private BigDecimal annualRevenue;
    private BigDecimal sumInsured;
    private String currency;
    private LocalDate coverageStart;
    private LocalDate coverageEnd;
    private BigDecimal targetPremium;
    private String riskDescription;

    private BigDecimal quotedPremium;
    private LocalDate quoteValidUntil;
    private String quoteTerms;
    private String policyNumber;
    private String closedReason;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant statusChangedAt;

    // two underwriters clicking "start review" at once: the second one gets a 409
    @Version
    private long version;

    protected Submission() {
    }

    public static Submission draft(User broker, String reference, SubmissionDetails details, Instant now) {
        if (!broker.isBroker()) {
            throw new ActionNotAllowedException("Only brokers create submissions");
        }
        var submission = new Submission();
        submission.id = UUID.randomUUID();
        submission.reference = reference;
        submission.broker = broker;
        submission.brokerage = broker.getBrokerage();
        submission.status = SubmissionStatus.DRAFT;
        submission.createdAt = now;
        submission.statusChangedAt = now;
        submission.apply(details, now);
        return submission;
    }

    // brokers fix things up while it's a draft or when the underwriter asked for more
    public void update(SubmissionDetails details, Instant now) {
        if (status != SubmissionStatus.DRAFT && status != SubmissionStatus.INFO_REQUESTED) {
            throw new ActionNotAllowedException("A submission can only be edited as a draft or when info is requested");
        }
        apply(details, now);
    }

    private void apply(SubmissionDetails d, Instant now) {
        lineOfBusiness = d.lineOfBusiness();
        insuredName = d.insuredName().trim();
        insuredCountry = d.insuredCountry() == null ? null : d.insuredCountry().toUpperCase();
        insuredIndustry = d.insuredIndustry();
        annualRevenue = d.annualRevenue();
        sumInsured = d.sumInsured();
        currency = d.currency() == null ? "EUR" : d.currency();
        coverageStart = d.coverageStart();
        coverageEnd = d.coverageEnd();
        targetPremium = d.targetPremium();
        riskDescription = d.riskDescription();
        updatedAt = now;
    }

    public SubmissionDetails details() {
        return new SubmissionDetails(lineOfBusiness, insuredName, insuredCountry, insuredIndustry, annualRevenue,
                sumInsured, currency, coverageStart, coverageEnd, targetPremium, riskDescription);
    }

    // the usual reasons an underwriter sends it back, caught before it leaves the broker
    public List<String> missingFields() {
        var missing = new ArrayList<String>();
        if (isBlank(insuredCountry)) {
            missing.add("insuredCountry");
        }
        if (isBlank(insuredIndustry)) {
            missing.add("insuredIndustry");
        }
        if (sumInsured == null) {
            missing.add("sumInsured");
        }
        if (coverageStart == null) {
            missing.add("coverageStart");
        }
        if (coverageEnd == null || (coverageStart != null && !coverageEnd.isAfter(coverageStart))) {
            missing.add("coverageEnd");
        }
        if (isBlank(riskDescription)) {
            missing.add("riskDescription");
        }
        return missing;
    }

    public boolean isVisibleTo(User viewer) {
        if (viewer.isBroker()) {
            return brokerage.getId().equals(viewer.getBrokerage().getId());
        }
        // drafts are the broker's business until they hit submit
        return status != SubmissionStatus.DRAFT;
    }

    public boolean canPerform(SubmissionAction action, User actor) {
        if (!isVisibleTo(actor) || !action.isAllowedFrom(status, actor.getRole())) {
            return false;
        }
        // once an underwriter picks it up, it's theirs
        return !actor.isUnderwriter() || underwriter == null || underwriter.getId().equals(actor.getId());
    }

    public List<SubmissionAction> availableActions(User viewer) {
        return Arrays.stream(SubmissionAction.values()).filter(a -> canPerform(a, viewer)).toList();
    }

    public SubmissionEvent perform(User actor, ActionRequest request, LocalDate today, Instant now) {
        SubmissionAction action = request.action();
        if (actor.isUnderwriter() && underwriter != null && !underwriter.getId().equals(actor.getId())) {
            throw new ActionNotAllowedException(underwriter.getFullName() + " is handling this submission");
        }
        if (!canPerform(action, actor)) {
            throw new ActionNotAllowedException("You can't " + action.name().toLowerCase().replace('_', ' ')
                    + " a submission that is " + status.name().toLowerCase().replace('_', ' '));
        }
        String note = isBlank(request.note()) ? null : request.note().trim();
        if (action.needsNote() && note == null) {
            throw new InvalidActionException("Add a note so the other side knows why");
        }
        switch (action) {
            case SUBMIT -> {
                var missing = missingFields();
                if (!missing.isEmpty()) {
                    throw new IncompleteSubmissionException(missing);
                }
            }
            case START_REVIEW -> underwriter = actor;
            case DECLINE -> {
                underwriter = actor;
                closedReason = note;
            }
            case QUOTE -> quote(request, today);
            case REQUEST_BIND -> {
                if (quoteValidUntil.isBefore(today)) {
                    throw new InvalidActionException("This quote expired on " + quoteValidUntil);
                }
            }
            // the policy number follows the submission number, easy to match up on the phone
            case BOOK -> policyNumber = reference.replaceFirst("^SUB-", "POL-");
            case REJECT_QUOTE, WITHDRAW, CANCEL -> closedReason = note;
            default -> {
            }
        }
        return moveTo(action.getTarget(), actor, note, now);
    }

    private void quote(ActionRequest request, LocalDate today) {
        if (request.premium() == null || request.premium().signum() <= 0) {
            throw new InvalidActionException("A quote needs a premium above zero");
        }
        if (request.validUntil() == null || request.validUntil().isBefore(today)) {
            throw new InvalidActionException("A quote needs a valid-until date from today on");
        }
        quotedPremium = request.premium();
        quoteValidUntil = request.validUntil();
        quoteTerms = isBlank(request.terms()) ? null : request.terms().trim();
    }

    // run by the hourly job, there's no user behind it
    public SubmissionEvent expireQuote(LocalDate today, Instant now) {
        if (status != SubmissionStatus.QUOTED || !quoteValidUntil.isBefore(today)) {
            throw new ActionNotAllowedException("Only a quote past its date can expire");
        }
        closedReason = "Quote expired on " + quoteValidUntil;
        return moveTo(SubmissionStatus.NOT_TAKEN_UP, null, closedReason, now);
    }

    private SubmissionEvent moveTo(SubmissionStatus target, User actor, String note, Instant now) {
        var event = new SubmissionEvent(this, status, target, actor, note, now);
        status = target;
        statusChangedAt = now;
        updatedAt = now;
        return event;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Brokerage getBrokerage() {
        return brokerage;
    }

    public User getBroker() {
        return broker;
    }

    public User getUnderwriter() {
        return underwriter;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public LineOfBusiness getLineOfBusiness() {
        return lineOfBusiness;
    }

    public String getInsuredName() {
        return insuredName;
    }

    public String getInsuredCountry() {
        return insuredCountry;
    }

    public String getInsuredIndustry() {
        return insuredIndustry;
    }

    public BigDecimal getAnnualRevenue() {
        return annualRevenue;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getCoverageStart() {
        return coverageStart;
    }

    public LocalDate getCoverageEnd() {
        return coverageEnd;
    }

    public BigDecimal getTargetPremium() {
        return targetPremium;
    }

    public String getRiskDescription() {
        return riskDescription;
    }

    public BigDecimal getQuotedPremium() {
        return quotedPremium;
    }

    public LocalDate getQuoteValidUntil() {
        return quoteValidUntil;
    }

    public String getQuoteTerms() {
        return quoteTerms;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getClosedReason() {
        return closedReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }
}
