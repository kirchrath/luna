package de.kirchrath.luna.appointment.outbound;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.appointment.config.WebSocketProperties;
import de.kirchrath.luna.appointment.dto.AppointmentChangedDto;
import de.kirchrath.luna.appointment.services.AppointmentChangedEvent;

/** Sends a signal to connected clients once the change of a appointment is committed. */
@Component
public class AppointmentChangeNotifier {

    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public AppointmentChangeNotifier(SimpMessageSendingOperations messagingTemplate, WebSocketProperties webSocketProperties) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketProperties = webSocketProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyChanged(AppointmentChangedEvent event) {
        messagingTemplate.convertAndSend(webSocketProperties.topics().appointments(), new AppointmentChangedDto(event.appointment().id()));
    }
}
