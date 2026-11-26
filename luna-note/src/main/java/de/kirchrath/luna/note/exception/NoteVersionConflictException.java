package de.kirchrath.luna.note.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class NoteVersionConflictException extends RuntimeException {

    public NoteVersionConflictException(UUID id, long expected, long actual) {
        super("Note " + id + " has version " + actual + ", but version " + expected + " was edited");
    }
}
