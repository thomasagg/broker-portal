package gr.brokerportal.submission;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SubmissionRepository extends JpaRepository<Submission, UUID>, JpaSpecificationExecutor<Submission> {

    // lists show broker, underwriter and brokerage names; one query instead of one per row
    @Override
    @EntityGraph(attributePaths = { "broker", "underwriter", "brokerage" })
    List<Submission> findAll(Specification<Submission> spec, Sort sort);

    @Query(value = "SELECT nextval('submission_ref_seq')", nativeQuery = true)
    long nextReferenceNumber();

    List<Submission> findByStatusAndQuoteValidUntilBefore(SubmissionStatus status, LocalDate date);

    interface StatusCount {
        SubmissionStatus getStatus();

        long getCount();
    }

    @Query("SELECT s.status AS status, COUNT(s) AS count FROM Submission s"
            + " WHERE s.status <> gr.brokerportal.submission.SubmissionStatus.DRAFT GROUP BY s.status")
    List<StatusCount> countByStatus();

    @Query("SELECT s.status AS status, COUNT(s) AS count FROM Submission s"
            + " WHERE s.brokerage.id = :brokerageId GROUP BY s.status")
    List<StatusCount> countByStatusForBrokerage(UUID brokerageId);

    // average hours from the first time a submission reached SUBMITTED to the first time it reached target
    @Query(value = """
            SELECT avg(extract(epoch FROM (t.reached - s.reached)) / 3600) AS hours, count(*) AS sample
            FROM (SELECT submission_id, min(created_at) AS reached FROM submission_event
                  WHERE to_status = 'SUBMITTED' GROUP BY submission_id) s
            JOIN (SELECT submission_id, min(created_at) AS reached FROM submission_event
                  WHERE to_status = :target GROUP BY submission_id) t USING (submission_id)
            WHERE t.reached >= :since
            """, nativeQuery = true)
    Turnaround turnaroundSince(String target, Instant since);

    interface Turnaround {
        Double getHours();

        long getSample();
    }
}
