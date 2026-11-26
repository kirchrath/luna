package de.kirchrath.luna.note.outbound;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.note.config.WebSocketProperties;
import de.kirchrath.luna.note.dto.NoteChangedDto;
import de.kirchrath.luna.note.services.NoteChangedEvent;

/** Sends a signal to connected clients once the change of a note is committed. */
@Component
public class NoteChangeNotifier {

    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public NoteChangeNotifier(SimpMessageSendingOperations messagingTemplate, WebSocketProperties webSocketProperties) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketProperties = webSocketProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyChanged(NoteChangedEvent event) {
        messagingTemplate.convertAndSend(webSocketProperties.topics().notes(), new NoteChangedDto(event.note().id()));
    }
}
