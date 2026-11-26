package de.kirchrath.luna.alert.outbound;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.alert.config.WebSocketProperties;
import de.kirchrath.luna.alert.dto.AlertChangedDto;
import de.kirchrath.luna.alert.services.AlertChangedEvent;

/** Sends a signal to connected clients once the change of a alert is committed. */
@Component
public class AlertChangeNotifier {

    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public AlertChangeNotifier(SimpMessageSendingOperations messagingTemplate, WebSocketProperties webSocketProperties) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketProperties = webSocketProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyChanged(AlertChangedEvent event) {
        messagingTemplate.convertAndSend(webSocketProperties.topics().alerts(), new AlertChangedDto(event.alert().id()));
    }
}
