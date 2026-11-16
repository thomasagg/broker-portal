package gr.brokerportal.submission;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import gr.brokerportal.user.Role;

public record SubmissionDetail(SubmissionResponse submission, List<Event> events, List<Message> messages) {

    public record Event(UUID id, SubmissionStatus fromStatus, SubmissionStatus toStatus, String actorName,
            String note, Instant createdAt) {

        static Event of(SubmissionEvent e) {
            return new Event(e.getId(), e.getFromStatus(), e.getToStatus(),
                    e.getActor() == null ? null : e.getActor().getFullName(), e.getNote(), e.getCreatedAt());
        }
    }

    public record Message(UUID id, String authorName, Role authorRole, String body, Instant createdAt) {

        static Message of(SubmissionMessage m) {
            return new Message(m.getId(), m.getAuthor().getFullName(), m.getAuthor().getRole(), m.getBody(),
                    m.getCreatedAt());
        }
    }
}
