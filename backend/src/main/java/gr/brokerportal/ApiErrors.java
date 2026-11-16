package gr.brokerportal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import gr.brokerportal.submission.ActionNotAllowedException;
import gr.brokerportal.submission.IncompleteSubmissionException;
import gr.brokerportal.submission.InvalidActionException;
import gr.brokerportal.submission.SubmissionNotFoundException;

@RestControllerAdvice
class ApiErrors {

    @ExceptionHandler
    ProblemDetail notFound(SubmissionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Submission not found");
    }

    @ExceptionHandler
    ProblemDetail notAllowed(ActionNotAllowedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail invalid(InvalidActionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // the web app ticks off these fields on the form
    @ExceptionHandler
    ProblemDetail incomplete(IncompleteSubmissionException e) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
        problem.setProperty("missingFields", e.getMissingFields());
        return problem;
    }

    @ExceptionHandler
    ProblemDetail changedMeanwhile(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Someone else changed this submission just now, reload and try again");
    }
}
