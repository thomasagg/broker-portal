package gr.brokerportal.submission;

public enum SubmissionStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    INFO_REQUESTED,
    QUOTED,
    BIND_REQUESTED,
    BOUND,
    BOOKED,
    DECLINED,
    NOT_TAKEN_UP,
    WITHDRAWN,
    CANCELLED;

    // BOOKED is the happy end, but a booked policy can still be cancelled
    public boolean isClosed() {
        return this == DECLINED || this == NOT_TAKEN_UP || this == WITHDRAWN || this == CANCELLED;
    }
}
