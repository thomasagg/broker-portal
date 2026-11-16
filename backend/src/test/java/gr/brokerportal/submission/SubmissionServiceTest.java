package gr.brokerportal.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gr.brokerportal.user.Brokerage;
import gr.brokerportal.user.User;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    static final ZoneId ATHENS = ZoneId.of("Europe/Athens");

    @Mock
    SubmissionRepository submissions;

    @Mock
    SubmissionEventRepository events;

    @Mock
    SubmissionMessageRepository messages;

    // 00:30 in Athens is still the 16th in UTC; the expiry date has to follow Athens
    Clock clock = Clock.fixed(Instant.parse("2026-11-16T22:30:00Z"), ATHENS);

    @Test
    void expiresQuotesUsingTheLocalDate() {
        LocalDate today = LocalDate.of(2026, 11, 17);
        Submission quoted = quotedUntil(today.minusDays(1));
        when(submissions.findByStatusAndQuoteValidUntilBefore(SubmissionStatus.QUOTED, today))
                .thenReturn(List.of(quoted));
        var service = new SubmissionService(submissions, events, messages, clock);

        int expired = service.expireQuotes();

        assertThat(expired).isEqualTo(1);
        var saved = ArgumentCaptor.forClass(SubmissionEvent.class);
        verify(events).save(saved.capture());
        assertThat(saved.getValue().getToStatus()).isEqualTo(SubmissionStatus.NOT_TAKEN_UP);
        assertThat(quoted.getStatus()).isEqualTo(SubmissionStatus.NOT_TAKEN_UP);
    }

    @Test
    void savesNothingWhenNoQuoteIsDue() {
        when(submissions.findByStatusAndQuoteValidUntilBefore(any(), any())).thenReturn(List.of());
        var service = new SubmissionService(submissions, events, messages, clock);

        assertThat(service.expireQuotes()).isZero();
        verify(events, never()).save(any());
    }

    private Submission quotedUntil(LocalDate validUntil) {
        User broker = User.broker("maria@aegean.example", "x", "Maria", new Brokerage("Aegean"));
        User underwriter = User.underwriter("anna@insurer.example", "x", "Anna");
        LocalDate quotedOn = validUntil.minusDays(10);
        Instant at = quotedOn.atStartOfDay(ATHENS).toInstant();
        Submission s = Submission.draft(broker, "SUB-2026-000001", SubmissionTest.complete(), at);
        s.perform(broker, ActionRequest.of(SubmissionAction.SUBMIT), quotedOn, at);
        s.perform(underwriter, ActionRequest.of(SubmissionAction.START_REVIEW), quotedOn, at);
        s.perform(underwriter, new ActionRequest(SubmissionAction.QUOTE, null, new BigDecimal("5000"), validUntil,
                null), quotedOn, at);
        return s;
    }
}
