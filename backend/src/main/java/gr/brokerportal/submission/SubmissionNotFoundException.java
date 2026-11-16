package gr.brokerportal.submission;

// also thrown for another brokerage's submission, so ids don't leak
public class SubmissionNotFoundException extends RuntimeException {
}
