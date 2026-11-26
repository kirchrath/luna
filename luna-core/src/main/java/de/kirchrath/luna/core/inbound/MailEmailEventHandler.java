package de.kirchrath.luna.core.inbound;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.dto.mail.EmailEventDto;
import de.kirchrath.luna.core.services.InboundEventApplicationService;

/** Emails of luna-mail; failures are retried via the retry topic and end up in the DLT (see KafkaConfig). */
@Component
public class MailEmailEventHandler {

    private final InboundEventApplicationService inboundEventApplicationService;

    public MailEmailEventHandler(InboundEventApplicationService inboundEventApplicationService) {
        this.inboundEventApplicationService = inboundEventApplicationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.mail-email}")
    public void handle(EmailEventDto event) {
        inboundEventApplicationService.createMailInboundEvent(event);
    }
}
