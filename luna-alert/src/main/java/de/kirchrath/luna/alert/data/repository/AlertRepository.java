package de.kirchrath.luna.alert.data.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import de.kirchrath.luna.alert.data.entity.Alert;
import de.kirchrath.luna.alert.data.entity.AlertSource;
import de.kirchrath.luna.alert.data.entity.AlertStatus;
import de.kirchrath.luna.alert.data.entity.LifeArea;

public interface AlertRepository extends CrudRepository<Alert, UUID>, PagingAndSortingRepository<Alert, UUID> {

    Optional<Alert> findBySourceAndSourceId(AlertSource source, UUID sourceId);

    Page<Alert> findByAreaIn(Collection<LifeArea> areas, Pageable pageable);

    Page<Alert> findByStatusInAndAreaIn(
            Collection<AlertStatus> statuses, Collection<LifeArea> areas, Pageable pageable);
}
