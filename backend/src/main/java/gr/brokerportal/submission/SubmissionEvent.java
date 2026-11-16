package gr.brokerportal.submission;

import java.time.Instant;
import java.util.UUID;

import gr.brokerportal.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class SubmissionEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Submission submission;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus toStatus;

    // null when the system did it, e.g. an expired quote
    @ManyToOne(fetch = FetchType.LAZY)
    private User actor;

    private String note;

    private Instant createdAt;

    protected SubmissionEvent() {
    }

    public SubmissionEvent(Submission submission, SubmissionStatus fromStatus, SubmissionStatus toStatus, User actor,
            String note, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.submission = submission;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.note = note;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public SubmissionStatus getFromStatus() {
        return fromStatus;
    }

    public SubmissionStatus getToStatus() {
        return toStatus;
    }

    public User getActor() {
        return actor;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
