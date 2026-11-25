package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.TaskElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.TaskElement;
import de.kirchrath.luna.core.data.repository.TaskElementRepository;
import de.kirchrath.luna.core.dto.TaskEventDataDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class TaskElementProcessor extends ElementProcessor<TaskEventDataDto, TaskElement> {

    private final TaskElementRepository taskElementRepository;
    private final TaskElementConverter taskElementConverter;

    public TaskElementProcessor(
            @Value("classpath:prompts/qualification/task.st") Resource prompt,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            TaskElementRepository taskElementRepository,
            TaskElementConverter taskElementConverter) {
        super(LunaElementType.TASK, "luna_core_task", prompt, TaskEventDataDto.class,
                elementQualificationClient, elementEventPublisher, validator);
        this.taskElementRepository = taskElementRepository;
        this.taskElementConverter = taskElementConverter;
    }

    @Override
    protected TaskElement toEntity(UUID outboundEventId, TaskEventDataDto data) {
        return taskElementConverter.toEntity(outboundEventId, data);
    }

    @Override
    protected TaskElement save(TaskElement element) {
        return taskElementRepository.save(element);
    }

    @Override
    protected Object toEventDto(OutboundEvent outboundEvent, TaskElement element) {
        return taskElementConverter.toEventDto(outboundEvent, element);
    }
}
