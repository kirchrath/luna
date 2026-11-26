package de.kirchrath.luna.task.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskSource;
import de.kirchrath.luna.task.data.entity.TaskStatus;

public interface TaskRepository extends CrudRepository<Task, UUID>, PagingAndSortingRepository<Task, UUID> {

    Optional<Task> findBySourceAndSourceId(TaskSource source, UUID sourceId);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
