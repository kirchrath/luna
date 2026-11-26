package de.kirchrath.luna.task.inbound;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.task.dto.core.TaskEventDto;
import de.kirchrath.luna.task.services.CoreTaskApplicationService;

/** Task events of luna-core; failures are retried via the retry topic and end up in the DLT (see KafkaConfig). */
@Component
public class CoreTaskEventHandler {

    private final CoreTaskApplicationService coreTaskApplicationService;

    public CoreTaskEventHandler(CoreTaskApplicationService coreTaskApplicationService) {
        this.coreTaskApplicationService = coreTaskApplicationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.core-task}")
    public void handle(TaskEventDto event) {
        coreTaskApplicationService.process(event);
    }
}
