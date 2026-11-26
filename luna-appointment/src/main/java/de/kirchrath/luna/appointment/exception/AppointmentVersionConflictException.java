package de.kirchrath.luna.appointment.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AppointmentVersionConflictException extends RuntimeException {

    public AppointmentVersionConflictException(UUID id, long expected, long actual) {
        super("Appointment " + id + " has version " + actual + ", but version " + expected + " was edited");
    }
}
