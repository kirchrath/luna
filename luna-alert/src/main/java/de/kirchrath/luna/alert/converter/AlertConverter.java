package de.kirchrath.luna.alert.converter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.alert.data.entity.Alert;
import de.kirchrath.luna.alert.data.entity.AlertSeverity;
import de.kirchrath.luna.alert.data.entity.AlertSource;
import de.kirchrath.luna.alert.data.entity.AlertStatus;
import de.kirchrath.luna.alert.data.entity.LifeArea;
import de.kirchrath.luna.alert.dto.AlertDto;
import de.kirchrath.luna.alert.dto.AlertEventDto;
import de.kirchrath.luna.alert.dto.AlertListItemDto;
import de.kirchrath.luna.alert.dto.AlertStatusDto;
import de.kirchrath.luna.alert.dto.LifeAreaDto;
import de.kirchrath.luna.alert.dto.core.AlertEventDataDto;

@Component
public class AlertConverter {

    /** Most severe first: the list is sorted ascending by this rank. */
    private static final Map<AlertSeverity, Integer> SEVERITY_RANKS =
            Map.of(AlertSeverity.CRITICAL, 0, AlertSeverity.WARNING, 1, AlertSeverity.INFO, 2);

    private final AlertMapper alertMapper;

    public AlertConverter(AlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    /** A new, open alert from an alert event of luna-core. */
    public Alert fromCore(de.kirchrath.luna.alert.dto.core.AlertEventDto event) {
        return applyCore(Alert.builder()
                .source(AlertSource.CORE)
                .sourceId(event.getId())
                .area(alertMapper.toEntity(event.getArea()))
                .status(AlertStatus.OPEN)
                .build(), event);
    }

    /** The content of a newer alert event of luna-core; status, origin and audit stay. */
    public Alert applyCore(Alert alert, de.kirchrath.luna.alert.dto.core.AlertEventDto event) {
        AlertEventDataDto data = event.getData();
        var severity = alertMapper.toEntity(data.getSeverity());
        return alert.toBuilder()
                .title(data.getTitle())
                .message(data.getMessage())
                .severity(severity)
                .severityRank(SEVERITY_RANKS.get(severity))
                .affectedSystem(data.getAffectedSystem())
                .build();
    }

    public AlertDto toDto(Alert alert) {
        return alertMapper.toDto(alert);
    }

    public AlertEventDto toEventDto(Alert alert) {
        return alertMapper.toEventDto(alert);
    }

    public AlertListItemDto toListItemDto(Alert alert) {
        return alertMapper.toListItemDto(alert);
    }

    public List<AlertStatus> toEntities(List<AlertStatusDto> statuses) {
        return statuses.stream().map(alertMapper::toEntity).toList();
    }

    /** The given areas, or all areas without any. */
    public Set<LifeArea> toAreas(List<LifeAreaDto> areas) {
        return areas.isEmpty()
                ? Set.of(LifeArea.values())
                : areas.stream().map(alertMapper::toEntity).collect(Collectors.toUnmodifiableSet());
    }
}
