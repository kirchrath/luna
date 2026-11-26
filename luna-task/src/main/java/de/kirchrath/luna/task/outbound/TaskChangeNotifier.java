package de.kirchrath.luna.task.outbound;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.task.config.WebSocketProperties;
import de.kirchrath.luna.task.dto.TaskChangedDto;
import de.kirchrath.luna.task.services.TaskChangedEvent;

/** Sends a signal to connected clients once the change of a task is committed. */
@Component
public class TaskChangeNotifier {

    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public TaskChangeNotifier(SimpMessageSendingOperations messagingTemplate, WebSocketProperties webSocketProperties) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketProperties = webSocketProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyChanged(TaskChangedEvent event) {
        messagingTemplate.convertAndSend(webSocketProperties.topics().tasks(), new TaskChangedDto(event.task().id()));
    }
}
