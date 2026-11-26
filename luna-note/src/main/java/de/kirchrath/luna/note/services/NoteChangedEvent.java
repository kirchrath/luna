package de.kirchrath.luna.note.services;

import de.kirchrath.luna.note.dto.NoteEventDto;

/** Published within the transaction after a note was created or changed; sent to Kafka and clients after commit. */
public record NoteChangedEvent(NoteEventDto note) {
}
