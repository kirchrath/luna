package de.kirchrath.luna.note.inbound;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.note.dto.core.NoteEventDto;
import de.kirchrath.luna.note.services.CoreNoteApplicationService;

/** Note events of luna-core; failures are retried via the retry topic and end up in the DLT (see KafkaConfig). */
@Component
public class CoreNoteEventHandler {

    private final CoreNoteApplicationService coreNoteApplicationService;

    public CoreNoteEventHandler(CoreNoteApplicationService coreNoteApplicationService) {
        this.coreNoteApplicationService = coreNoteApplicationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.core-note}")
    public void handle(NoteEventDto event) {
        coreNoteApplicationService.process(event);
    }
}
