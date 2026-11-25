package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.TaskElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.TaskEventDataDto;
import de.kirchrath.luna.core.dto.TaskEventDto;

@Component
public class TaskElementConverter {

    private final TaskElementMapper taskElementMapper;

    public TaskElementConverter(TaskElementMapper taskElementMapper) {
        this.taskElementMapper = taskElementMapper;
    }

    public TaskElement toEntity(UUID outboundEventId, TaskEventDataDto data) {
        return taskElementMapper.toEntity(outboundEventId, data);
    }

    public TaskEventDto toEventDto(OutboundEvent outboundEvent, TaskElement element) {
        return taskElementMapper.toEventDto(outboundEvent, element);
    }
}
