package de.kirchrath.luna.alert.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A resolved alert has to be reopened before it can be acknowledged again. */
@ResponseStatus(HttpStatus.CONFLICT)
public class AlertTransitionException extends RuntimeException {

    public AlertTransitionException(UUID id, String from, String to) {
        super("Alert " + id + " cannot change from " + from + " to " + to);
    }
}
