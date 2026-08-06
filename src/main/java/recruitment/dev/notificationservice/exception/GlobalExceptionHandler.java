package recruitment.dev.notificationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Validation failed");
        detail.setDetail(ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid notification request"));
        return detail;
    }

    @ExceptionHandler(NotificationDeliveryException.class)
    public ProblemDetail handleDelivery(NotificationDeliveryException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        detail.setTitle("Notification delivery failed");
        detail.setDetail(ex.getMessage());
        return detail;
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ProblemDetail handleNotFound(NotificationNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("Notification not found");
        detail.setDetail(ex.getMessage());
        return detail;
    }
}
