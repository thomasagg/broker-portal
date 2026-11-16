package gr.brokerportal.submission;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// premium, validUntil and terms are only read for QUOTE
public record ActionRequest(
        @NotNull SubmissionAction action,
        @Size(max = 2000) String note,
        BigDecimal premium,
        LocalDate validUntil,
        @Size(max = 5000) String terms) {

    public static ActionRequest of(SubmissionAction action) {
        return new ActionRequest(action, null, null, null, null);
    }

    public static ActionRequest of(SubmissionAction action, String note) {
        return new ActionRequest(action, note, null, null, null);
    }
}
