package gr.brokerportal.submission;

import java.util.List;

public class IncompleteSubmissionException extends RuntimeException {

    private final List<String> missingFields;

    public IncompleteSubmissionException(List<String> missingFields) {
        super("Fill in " + String.join(", ", missingFields) + " before submitting");
        this.missingFields = missingFields;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
