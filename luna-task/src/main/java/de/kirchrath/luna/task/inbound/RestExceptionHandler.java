package de.kirchrath.luna.task.inbound;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A task changed by someone else in the meantime is a client conflict, not a server error. The status is not declared
 * per annotation, otherwise springdoc would add it to every operation.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Void> handleOptimisticLockingFailure() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
