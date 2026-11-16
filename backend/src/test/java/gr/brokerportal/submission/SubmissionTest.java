package gr.brokerportal.submission;

import static gr.brokerportal.submission.SubmissionAction.BIND;
import static gr.brokerportal.submission.SubmissionAction.BOOK;
import static gr.brokerportal.submission.SubmissionAction.QUOTE;
import static gr.brokerportal.submission.SubmissionAction.REJECT_QUOTE;
import static gr.brokerportal.submission.SubmissionAction.REQUEST_BIND;
import static gr.brokerportal.submission.SubmissionAction.REQUEST_INFO;
import static gr.brokerportal.submission.SubmissionAction.START_REVIEW;
import static gr.brokerportal.submission.SubmissionAction.SUBMIT;
import static gr.brokerportal.submission.SubmissionAction.WITHDRAW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import gr.brokerportal.user.Brokerage;
import gr.brokerportal.user.User;

class SubmissionTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 17);
    static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

    Brokerage aegean = new Brokerage("Aegean");
    User maria = User.broker("maria@aegean.example", "x", "Maria", aegean);
    User nikos = User.broker("nikos@aegean.example", "x", "Nikos", aegean);
    User eleni = User.broker("eleni@northern.example", "x", "Eleni", new Brokerage("Northern"));
    User anna = User.underwriter("anna@insurer.example", "x", "Anna");
    User george = User.underwriter("george@insurer.example", "x", "George");

    static SubmissionDetails complete() {
        return new SubmissionDetails(LineOfBusiness.PROPERTY, "Thermaikos Logistics", "GR", "Warehousing", null,
                new BigDecimal("4500000"), "EUR", TODAY.plusDays(14), TODAY.plusDays(14).plusYears(1), null,
                "Two sprinklered warehouses");
    }

    Submission draft(SubmissionDetails details) {
        return Submission.draft(maria, "SUB-2026-000001", details, NOW);
    }

    Submission quoted(LocalDate validUntil) {
        Submission s = draft(complete());
        s.perform(maria, ActionRequest.of(SUBMIT), TODAY, NOW);
        s.perform(anna, ActionRequest.of(START_REVIEW), TODAY, NOW);
        s.perform(anna, new ActionRequest(QUOTE, null, new BigDecimal("12000"), validUntil, null), TODAY, NOW);
        return s;
    }

    @Test
    void goesAllTheWayFromDraftToBooked() {
        Submission s = quoted(TODAY.plusDays(30));

        s.perform(maria, ActionRequest.of(REQUEST_BIND), TODAY, NOW);
        s.perform(anna, ActionRequest.of(BIND), TODAY, NOW);
        SubmissionEvent booked = s.perform(anna, ActionRequest.of(BOOK), TODAY, NOW);

        assertThat(s.getStatus()).isEqualTo(SubmissionStatus.BOOKED);
        assertThat(s.getPolicyNumber()).isEqualTo("POL-2026-000001");
        assertThat(s.getUnderwriter()).isSameAs(anna);
        assertThat(booked.getFromStatus()).isEqualTo(SubmissionStatus.BOUND);
        assertThat(booked.getToStatus()).isEqualTo(SubmissionStatus.BOOKED);
    }

    @Test
    void listsWhatIsMissingBeforeItCanBeSubmitted() {
        Submission s = draft(new SubmissionDetails(LineOfBusiness.CYBER, "Olympus Dental", null, null, null, null,
                null, TODAY.plusDays(10), TODAY.plusDays(5), null, " "));

        assertThatThrownBy(() -> s.perform(maria, ActionRequest.of(SUBMIT), TODAY, NOW))
                .isInstanceOfSatisfying(IncompleteSubmissionException.class, e -> assertThat(e.getMissingFields())
                        .containsExactly("insuredCountry", "insuredIndustry", "sumInsured", "coverageEnd",
                                "riskDescription"));
        assertThat(s.getStatus()).isEqualTo(SubmissionStatus.DRAFT);
    }

    @Test
    void brokersCannotQuoteTheirOwnSubmission() {
        Submission s = draft(complete());
        s.perform(maria, ActionRequest.of(SUBMIT), TODAY, NOW);

        assertThatThrownBy(() -> s.perform(maria,
                new ActionRequest(QUOTE, null, BigDecimal.TEN, TODAY.plusDays(5), null), TODAY, NOW))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void onceAnUnderwriterPicksItUpOthersCannotAct() {
        Submission s = draft(complete());
        s.perform(maria, ActionRequest.of(SUBMIT), TODAY, NOW);
        s.perform(anna, ActionRequest.of(START_REVIEW), TODAY, NOW);

        assertThat(s.availableActions(george)).isEmpty();
        assertThatThrownBy(() -> s.perform(george, ActionRequest.of(REQUEST_INFO, "why?"), TODAY, NOW))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void askingForInfoNeedsANote() {
        Submission s = draft(complete());
        s.perform(maria, ActionRequest.of(SUBMIT), TODAY, NOW);
        s.perform(anna, ActionRequest.of(START_REVIEW), TODAY, NOW);

        assertThatThrownBy(() -> s.perform(anna, ActionRequest.of(REQUEST_INFO, "  "), TODAY, NOW))
                .isInstanceOf(InvalidActionException.class);
    }

    @Test
    void anExpiredQuoteCannotBeBound() {
        Submission s = quoted(TODAY.plusDays(3));

        assertThatThrownBy(() -> s.perform(maria, ActionRequest.of(REQUEST_BIND), TODAY.plusDays(4), NOW))
                .isInstanceOf(InvalidActionException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void anOldQuoteExpiresToNotTakenUp() {
        Submission s = quoted(TODAY.plusDays(3));

        SubmissionEvent event = s.expireQuote(TODAY.plusDays(4), NOW);

        assertThat(s.getStatus()).isEqualTo(SubmissionStatus.NOT_TAKEN_UP);
        assertThat(event.getActor()).isNull();
        assertThat(s.getClosedReason()).contains("expired");
    }

    @Test
    void aQuoteStillValidTodayDoesNotExpire() {
        Submission s = quoted(TODAY.plusDays(3));

        assertThatThrownBy(() -> s.expireQuote(TODAY.plusDays(3), NOW))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void aColleagueFromTheSameBrokerageCanTakeOver() {
        Submission s = quoted(TODAY.plusDays(30));

        assertThat(s.availableActions(nikos)).containsExactly(REQUEST_BIND, REJECT_QUOTE, WITHDRAW);
        assertThat(s.isVisibleTo(eleni)).isFalse();
    }

    @Test
    void underwritersDoNotSeeDrafts() {
        Submission s = draft(complete());

        assertThat(s.isVisibleTo(anna)).isFalse();
        s.perform(maria, ActionRequest.of(SUBMIT), TODAY, NOW);
        assertThat(s.isVisibleTo(anna)).isTrue();
    }

    @Test
    void canOnlyBeEditedAsDraftOrWhenInfoIsRequested() {
        Submission s = draft(complete());
        s.perform(maria, ActionRequest.of(SUBMIT), TODAY, NOW);

        assertThatThrownBy(() -> s.update(complete(), NOW)).isInstanceOf(ActionNotAllowedException.class);

        s.perform(anna, ActionRequest.of(START_REVIEW), TODAY, NOW);
        s.perform(anna, ActionRequest.of(REQUEST_INFO, "Sprinkler certificate please"), TODAY, NOW);
        s.update(complete(), NOW);
        assertThat(s.getStatus()).isEqualTo(SubmissionStatus.INFO_REQUESTED);
    }
}
