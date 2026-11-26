package de.kirchrath.luna.alert.inbound;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.alert.dto.core.AlertEventDto;
import de.kirchrath.luna.alert.services.CoreAlertApplicationService;

/** Alert events of luna-core; failures are retried via the retry topic and end up in the DLT (see KafkaConfig). */
@Component
public class CoreAlertEventHandler {

    private final CoreAlertApplicationService coreAlertApplicationService;

    public CoreAlertEventHandler(CoreAlertApplicationService coreAlertApplicationService) {
        this.coreAlertApplicationService = coreAlertApplicationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.core-alert}")
    public void handle(AlertEventDto event) {
        coreAlertApplicationService.process(event);
    }
}
