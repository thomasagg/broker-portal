package gr.brokerportal.submission;

import static gr.brokerportal.submission.SubmissionStatus.BIND_REQUESTED;
import static gr.brokerportal.submission.SubmissionStatus.BOOKED;
import static gr.brokerportal.submission.SubmissionStatus.BOUND;
import static gr.brokerportal.submission.SubmissionStatus.CANCELLED;
import static gr.brokerportal.submission.SubmissionStatus.DECLINED;
import static gr.brokerportal.submission.SubmissionStatus.DRAFT;
import static gr.brokerportal.submission.SubmissionStatus.INFO_REQUESTED;
import static gr.brokerportal.submission.SubmissionStatus.IN_REVIEW;
import static gr.brokerportal.submission.SubmissionStatus.NOT_TAKEN_UP;
import static gr.brokerportal.submission.SubmissionStatus.QUOTED;
import static gr.brokerportal.submission.SubmissionStatus.SUBMITTED;
import static gr.brokerportal.submission.SubmissionStatus.WITHDRAWN;

import java.util.EnumSet;
import java.util.Set;

import gr.brokerportal.user.Role;

// the whole lifecycle in one place: who can do it, from where, and where it lands
public enum SubmissionAction {
    SUBMIT(Role.BROKER, SUBMITTED, false, DRAFT),
    START_REVIEW(Role.UNDERWRITER, IN_REVIEW, false, SUBMITTED),
    REQUEST_INFO(Role.UNDERWRITER, INFO_REQUESTED, true, IN_REVIEW),
    PROVIDE_INFO(Role.BROKER, IN_REVIEW, true, INFO_REQUESTED),
    QUOTE(Role.UNDERWRITER, QUOTED, false, IN_REVIEW),
    DECLINE(Role.UNDERWRITER, DECLINED, true, SUBMITTED, IN_REVIEW, INFO_REQUESTED),
    REQUEST_BIND(Role.BROKER, BIND_REQUESTED, false, QUOTED),
    REJECT_QUOTE(Role.BROKER, NOT_TAKEN_UP, true, QUOTED),
    BIND(Role.UNDERWRITER, BOUND, false, BIND_REQUESTED),
    BOOK(Role.UNDERWRITER, BOOKED, false, BOUND),
    WITHDRAW(Role.BROKER, WITHDRAWN, true, DRAFT, SUBMITTED, IN_REVIEW, INFO_REQUESTED, QUOTED, BIND_REQUESTED),
    CANCEL(Role.UNDERWRITER, CANCELLED, true, BOUND, BOOKED);

    private final Role role;
    private final SubmissionStatus target;
    private final boolean needsNote;
    private final Set<SubmissionStatus> from;

    SubmissionAction(Role role, SubmissionStatus target, boolean needsNote, SubmissionStatus first,
            SubmissionStatus... rest) {
        this.role = role;
        this.target = target;
        this.needsNote = needsNote;
        this.from = EnumSet.of(first, rest);
    }

    public boolean isAllowedFrom(SubmissionStatus status, Role actorRole) {
        return role == actorRole && from.contains(status);
    }

    public Role getRole() {
        return role;
    }

    public SubmissionStatus getTarget() {
        return target;
    }

    public boolean needsNote() {
        return needsNote;
    }

    // being able to decline or withdraw doesn't mean the ball is in your court
    public boolean closesSubmission() {
        return target.isClosed();
    }
}
