package gr.brokerportal.submission;

// wrong status, wrong role, or another underwriter owns it
public class ActionNotAllowedException extends RuntimeException {

    public ActionNotAllowedException(String message) {
        super(message);
    }
}
