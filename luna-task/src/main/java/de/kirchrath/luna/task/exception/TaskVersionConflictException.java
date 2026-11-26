package de.kirchrath.luna.task.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TaskVersionConflictException extends RuntimeException {

    public TaskVersionConflictException(UUID id, long expected, long actual) {
        super("Task " + id + " has version " + actual + ", but version " + expected + " was edited");
    }
}
