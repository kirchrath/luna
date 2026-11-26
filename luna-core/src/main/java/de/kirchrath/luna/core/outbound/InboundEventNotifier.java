package de.kirchrath.luna.core.outbound;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.core.config.WebSocketProperties;
import de.kirchrath.luna.core.dto.InboundEventChangedDto;
import de.kirchrath.luna.core.services.InboundEventStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;

/** Sends a signal to connected clients once the status change of an inbound event is committed. */
@Slf4j
@Component
public class InboundEventNotifier {

    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public InboundEventNotifier(
            SimpMessageSendingOperations messagingTemplate, WebSocketProperties webSocketProperties) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketProperties = webSocketProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyStatusChanged(InboundEventStatusChangedEvent event) {
        messagingTemplate.convertAndSend(
                webSocketProperties.topics().inboundEvents(), new InboundEventChangedDto(event.inboundEventId()));
        log.debug("Notified clients about the status change of inbound event {}", event.inboundEventId());
    }
}
