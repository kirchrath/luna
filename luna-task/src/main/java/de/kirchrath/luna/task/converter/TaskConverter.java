package de.kirchrath.luna.task.converter;

import java.util.Set;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.task.data.entity.LifeArea;
import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskSource;
import de.kirchrath.luna.task.data.entity.TaskStatus;
import de.kirchrath.luna.task.dto.LifeAreaDto;
import de.kirchrath.luna.task.dto.TaskDto;
import de.kirchrath.luna.task.dto.TaskEventDto;
import de.kirchrath.luna.task.dto.TaskListItemDto;
import de.kirchrath.luna.task.dto.TaskStatusDto;
import de.kirchrath.luna.task.dto.TaskUpdateDto;
import de.kirchrath.luna.task.dto.core.TaskEventDataDto;

@Component
public class TaskConverter {

    private final TaskMapper taskMapper;

    public TaskConverter(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /** A new, unedited and open task from a task event of luna-core. */
    public Task fromCore(de.kirchrath.luna.task.dto.core.TaskEventDto event) {
        return applyCore(Task.builder()
                .source(TaskSource.CORE)
                .sourceId(event.getId())
                .area(taskMapper.toEntity(event.getArea()))
                .status(TaskStatus.OPEN)
                .edited(false)
                .build(), event);
    }

    /** The data of a newer task event of luna-core; status, origin and audit stay. */
    public Task applyCore(Task task, de.kirchrath.luna.task.dto.core.TaskEventDto event) {
        TaskEventDataDto data = event.getData();
        return task.toBuilder()
                .title(data.getTitle())
                .description(data.getDescription())
                .dueDate(data.getDueDate())
                .priority(taskMapper.toEntity(data.getPriority()))
                .build();
    }

    /** The data edited by the user; the task counts as edited from now on. */
    public Task applyUpdate(Task task, TaskUpdateDto update) {
        return task.toBuilder()
                .title(update.title())
                .description(update.description())
                .dueDate(update.dueDate())
                .priority(taskMapper.toEntity(update.priority()))
                .edited(true)
                .build();
    }

    public TaskDto toDto(Task task) {
        return taskMapper.toDto(task);
    }

    public TaskEventDto toEventDto(Task task) {
        return taskMapper.toEventDto(task);
    }

    public TaskListItemDto toListItemDto(Task task) {
        return taskMapper.toListItemDto(task);
    }

    public TaskStatus toEntity(TaskStatusDto status) {
        return taskMapper.toEntity(status);
    }

    /** Only the given area, or all areas without one. */
    public Set<LifeArea> toEntities(LifeAreaDto area) {
        return area == null ? Set.of(LifeArea.values()) : Set.of(taskMapper.toEntity(area));
    }
}
