package de.kirchrath.luna.task.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.task.data.entity.LifeArea;
import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskPriority;
import de.kirchrath.luna.task.data.entity.TaskStatus;
import de.kirchrath.luna.task.dto.AuditDto;
import de.kirchrath.luna.task.dto.LifeAreaDto;
import de.kirchrath.luna.task.dto.TaskDataDto;
import de.kirchrath.luna.task.dto.TaskDto;
import de.kirchrath.luna.task.dto.TaskEventDto;
import de.kirchrath.luna.task.dto.TaskListItemDto;
import de.kirchrath.luna.task.dto.TaskPriorityDto;
import de.kirchrath.luna.task.dto.TaskStatusDto;
import de.kirchrath.luna.task.dto.core.TaskEventDataDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskMapper {

    TaskDataDto toDataDto(Task task);

    AuditDto toAuditDto(Task task);

    @Mapping(target = "data", source = "task")
    @Mapping(target = "audit", source = "task")
    TaskDto toDto(Task task);

    @Mapping(target = "audit", source = "task")
    @Mapping(target = "data", source = "task")
    TaskEventDto toEventDto(Task task);

    TaskListItemDto toListItemDto(Task task);

    TaskStatus toEntity(TaskStatusDto status);

    TaskPriority toEntity(TaskPriorityDto priority);

    TaskPriority toEntity(TaskEventDataDto.PriorityEnum priority);

    LifeArea toEntity(LifeAreaDto area);

    LifeArea toEntity(de.kirchrath.luna.task.dto.core.TaskEventDto.AreaEnum area);
}
