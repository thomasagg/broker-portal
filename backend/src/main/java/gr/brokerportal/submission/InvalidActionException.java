package gr.brokerportal.submission;

// the action is allowed, but what came with it isn't (no note, negative premium, expired quote)
public class InvalidActionException extends RuntimeException {

    public InvalidActionException(String message) {
        super(message);
    }
}
