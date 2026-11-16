package gr.brokerportal.submission;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionEventRepository extends JpaRepository<SubmissionEvent, UUID> {

    @EntityGraph(attributePaths = "actor")
    List<SubmissionEvent> findBySubmissionIdOrderByCreatedAt(UUID submissionId);
}
