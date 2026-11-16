package gr.brokerportal.submission;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// what the broker fills in; a draft can be half empty, SUBMIT checks it's complete
public record SubmissionDetails(
        @NotNull LineOfBusiness lineOfBusiness,
        @NotBlank @Size(max = 200) String insuredName,
        @Size(max = 2) String insuredCountry,
        @Size(max = 100) String insuredIndustry,
        @Positive BigDecimal annualRevenue,
        @Positive BigDecimal sumInsured,
        @Pattern(regexp = "[A-Z]{3}") String currency,
        LocalDate coverageStart,
        LocalDate coverageEnd,
        @Positive BigDecimal targetPremium,
        @Size(max = 5000) String riskDescription) {
}
