package de.kirchrath.luna.core.outbound;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import de.kirchrath.luna.core.config.WebSocketProperties;
import de.kirchrath.luna.core.dto.InboundEventChangedDto;
import de.kirchrath.luna.core.services.InboundEventStatusChangedEvent;

class InboundEventNotifierTest {

    private final SimpMessageSendingOperations messagingTemplate = mock(SimpMessageSendingOperations.class);
    private final InboundEventNotifier inboundEventNotifier = new InboundEventNotifier(
            messagingTemplate,
            new WebSocketProperties("/ws", "/topic", new WebSocketProperties.Topics("/topic/inbound-events")));

    @Test
    void thatAStatusChangeShouldResultIntoASignalOnTheTopicOfInboundEvents() {
        // given
        var id = UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f");

        // when
        inboundEventNotifier.notifyStatusChanged(new InboundEventStatusChangedEvent(id));

        // then
        verify(messagingTemplate).convertAndSend("/topic/inbound-events", new InboundEventChangedDto(id));
    }
}
