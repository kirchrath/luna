package de.kirchrath.luna.task.services;

import de.kirchrath.luna.task.dto.TaskEventDto;

/** Published within the transaction after a task was created or changed; sent to Kafka and clients after commit. */
public record TaskChangedEvent(TaskEventDto task) {
}
