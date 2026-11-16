package gr.brokerportal.submission;

import java.time.Instant;
import java.util.UUID;

import gr.brokerportal.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class SubmissionMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    private String body;

    private Instant createdAt;

    protected SubmissionMessage() {
    }

    public SubmissionMessage(Submission submission, User author, String body, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.submission = submission;
        this.author = author;
        this.body = body.trim();
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
