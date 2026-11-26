package de.kirchrath.luna.task.services;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.task.converter.TaskConverter;
import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskStatus;
import de.kirchrath.luna.task.data.repository.TaskRepository;
import de.kirchrath.luna.task.dto.TaskDto;
import de.kirchrath.luna.task.dto.TaskListDto;
import de.kirchrath.luna.task.dto.TaskStatusDto;
import de.kirchrath.luna.task.dto.TaskUpdateDto;
import de.kirchrath.luna.task.exception.TaskNotFoundException;
import de.kirchrath.luna.task.exception.TaskVersionConflictException;

@Service
@Transactional
public class TaskDomainService {

    /** Due tasks first (tasks without due date last), then in the order they arrived. */
    private static final Sort LIST_ORDER =
            Sort.by(Sort.Order.asc(Task::dueDate).nullsLast(), Sort.Order.asc(Task::createdDate));

    private final TaskRepository taskRepository;
    private final TaskConverter taskConverter;
    private final TimeProvider timeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public TaskDomainService(
            TaskRepository taskRepository,
            TaskConverter taskConverter,
            TimeProvider timeProvider,
            ApplicationEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.taskConverter = taskConverter;
        this.timeProvider = timeProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public TaskListDto listTasks(TaskStatusDto status, Pageable pageable) {
        var pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LIST_ORDER);
        var tasks = status == null
                ? taskRepository.findAll(pageRequest)
                : taskRepository.findByStatus(taskConverter.toEntity(status), pageRequest);
        return new TaskListDto(
                tasks.map(taskConverter::toListItemDto).getContent(), tasks.getTotalPages(), tasks.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TaskDto getTask(UUID id) {
        return taskConverter.toDto(findTask(id));
    }

    public TaskDto updateTask(UUID id, TaskUpdateDto update) {
        var task = findTask(id);
        if (!task.version().equals(update.version())) {
            throw new TaskVersionConflictException(id, update.version(), task.version());
        }
        return save(taskConverter.applyUpdate(task, update));
    }

    public TaskDto completeTask(UUID id) {
        var task = findTask(id);
        if (task.status() == TaskStatus.DONE) {
            return taskConverter.toDto(task);
        }
        return save(task.toBuilder().status(TaskStatus.DONE).completedDate(timeProvider.now()).edited(true).build());
    }

    public TaskDto reopenTask(UUID id) {
        var task = findTask(id);
        if (task.status() == TaskStatus.OPEN) {
            return taskConverter.toDto(task);
        }
        return save(task.toBuilder().status(TaskStatus.OPEN).completedDate(null).edited(true).build());
    }

    private Task findTask(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private TaskDto save(Task task) {
        var saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskChangedEvent(taskConverter.toEventDto(saved)));
        return taskConverter.toDto(saved);
    }
}
