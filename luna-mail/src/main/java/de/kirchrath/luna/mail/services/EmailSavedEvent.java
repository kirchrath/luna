package de.kirchrath.luna.mail.services;

import de.kirchrath.luna.mail.dto.EmailEventDto;

/** Raised within the transaction that saved an email; it is published once the transaction is committed. */
public record EmailSavedEvent(EmailEventDto email) {
}
