package de.kirchrath.luna.appointment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AppointmentEndBeforeStartException extends RuntimeException {

    public AppointmentEndBeforeStartException() {
        super("The end of an appointment must not be before its start");
    }
}
