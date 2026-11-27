package de.kirchrath.luna.task.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.kirchrath.luna.task.dto.LifeAreaDto;
import de.kirchrath.luna.task.dto.TaskDto;
import de.kirchrath.luna.task.dto.TaskListDto;
import de.kirchrath.luna.task.dto.TaskStatusDto;
import de.kirchrath.luna.task.dto.TaskUpdateDto;

@Service
public class TaskApplicationService {

    private final TaskDomainService taskDomainService;

    public TaskApplicationService(TaskDomainService taskDomainService) {
        this.taskDomainService = taskDomainService;
    }

    public TaskListDto listTasks(TaskStatusDto status, List<LifeAreaDto> areas, Pageable pageable) {
        return taskDomainService.listTasks(status, areas, pageable);
    }

    public TaskDto getTask(UUID id) {
        return taskDomainService.getTask(id);
    }

    public TaskDto updateTask(UUID id, TaskUpdateDto update) {
        return taskDomainService.updateTask(id, update);
    }

    public TaskDto completeTask(UUID id) {
        return taskDomainService.completeTask(id);
    }

    public TaskDto reopenTask(UUID id) {
        return taskDomainService.reopenTask(id);
    }
}
