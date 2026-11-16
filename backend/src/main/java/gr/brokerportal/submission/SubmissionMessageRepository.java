package gr.brokerportal.submission;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionMessageRepository extends JpaRepository<SubmissionMessage, UUID> {

    @EntityGraph(attributePaths = "author")
    List<SubmissionMessage> findBySubmissionIdOrderByCreatedAt(UUID submissionId);
}
