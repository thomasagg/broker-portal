package gr.brokerportal.submission;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import gr.brokerportal.user.User;

public record SubmissionResponse(
        UUID id,
        String reference,
        SubmissionStatus status,
        LineOfBusiness lineOfBusiness,
        String insuredName,
        String insuredCountry,
        String insuredIndustry,
        BigDecimal annualRevenue,
        BigDecimal sumInsured,
        String currency,
        LocalDate coverageStart,
        LocalDate coverageEnd,
        BigDecimal targetPremium,
        String riskDescription,
        BigDecimal quotedPremium,
        LocalDate quoteValidUntil,
        String quoteTerms,
        String policyNumber,
        String closedReason,
        String brokerageName,
        String brokerName,
        String underwriterName,
        Instant createdAt,
        Instant updatedAt,
        Instant statusChangedAt,
        long daysInStatus,
        List<String> missingFields,
        List<SubmissionAction> availableActions) {

    // the actions depend on who's looking, so the web app never has to repeat the rules
    static SubmissionResponse of(Submission s, User viewer, Instant now) {
        return new SubmissionResponse(s.getId(), s.getReference(), s.getStatus(), s.getLineOfBusiness(),
                s.getInsuredName(), s.getInsuredCountry(), s.getInsuredIndustry(), s.getAnnualRevenue(),
                s.getSumInsured(), s.getCurrency(), s.getCoverageStart(), s.getCoverageEnd(), s.getTargetPremium(),
                s.getRiskDescription(), s.getQuotedPremium(), s.getQuoteValidUntil(), s.getQuoteTerms(),
                s.getPolicyNumber(), s.getClosedReason(), s.getBrokerage().getName(), s.getBroker().getFullName(),
                s.getUnderwriter() == null ? null : s.getUnderwriter().getFullName(), s.getCreatedAt(),
                s.getUpdatedAt(), s.getStatusChangedAt(), ChronoUnit.DAYS.between(s.getStatusChangedAt(), now),
                s.missingFields(), s.availableActions(viewer));
    }
}
