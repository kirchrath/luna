package de.kirchrath.luna.task.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.task.converter.TaskConverter;
import de.kirchrath.luna.task.data.entity.CoreTaskProjection;
import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskSource;
import de.kirchrath.luna.task.data.repository.CoreTaskProjectionRepository;
import de.kirchrath.luna.task.data.repository.TaskRepository;
import de.kirchrath.luna.task.dto.core.TaskEventDto;
import lombok.extern.slf4j.Slf4j;

/** Processes task events of luna-core: only newer versions pass the projection and create or update the task. */
@Slf4j
@Service
@Transactional
public class CoreTaskDomainService {

    private final CoreTaskProjectionRepository projectionRepository;
    private final TaskRepository taskRepository;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final TaskConverter taskConverter;
    private final ApplicationEventPublisher eventPublisher;

    public CoreTaskDomainService(
            CoreTaskProjectionRepository projectionRepository,
            TaskRepository taskRepository,
            JdbcAggregateTemplate aggregateTemplate,
            TaskConverter taskConverter,
            ApplicationEventPublisher eventPublisher) {
        this.projectionRepository = projectionRepository;
        this.taskRepository = taskRepository;
        this.aggregateTemplate = aggregateTemplate;
        this.taskConverter = taskConverter;
        this.eventPublisher = eventPublisher;
    }

    public void process(TaskEventDto event) {
        var coreVersion = event.getAudit().getVersion();
        var projection = projectionRepository.findByCoreTaskId(event.getId());
        if (projection.isEmpty()) {
            // The projection has no optimistic lock, so Spring Data cannot tell a new one from an existing one.
            aggregateTemplate.insert(new CoreTaskProjection(null, event.getId(), coreVersion));
            publish(taskRepository.save(taskConverter.fromCore(event)));
            return;
        }
        if (projection.get().version() >= coreVersion) {
            log.debug("Ignored core task {} in version {}; version {} was processed already",
                    event.getId(), coreVersion, projection.get().version());
            return;
        }
        projectionRepository.save(projection.get().withVersion(coreVersion));
        var task = taskRepository.findBySourceAndSourceId(TaskSource.CORE, event.getId()).orElseThrow();
        if (task.edited()) {
            log.debug("Kept edited task {}; core task {} version {} is not applied", task.id(), event.getId(),
                    coreVersion);
            return;
        }
        publish(taskRepository.save(taskConverter.applyCore(task, event)));
    }

    private void publish(Task task) {
        eventPublisher.publishEvent(new TaskChangedEvent(taskConverter.toEventDto(task)));
    }
}
