package de.kirchrath.luna.alert.inbound;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.alert.config.TestcontainersConfig;
import de.kirchrath.luna.alert.data.entity.Alert;
import de.kirchrath.luna.alert.data.entity.AlertSeverity;
import de.kirchrath.luna.alert.data.entity.AlertSource;
import de.kirchrath.luna.alert.data.entity.AlertStatus;
import de.kirchrath.luna.alert.data.repository.AlertRepository;
import de.kirchrath.luna.alert.data.repository.CoreAlertProjectionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AlertControllerTest {

    private static final OffsetDateTime EARLIER = OffsetDateTime.parse("2026-11-26T10:00:00+01:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CoreAlertProjectionRepository projectionRepository;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void thatListingAlertsShouldResultIntoTheMostSevereFirstAndThenTheNewest() throws Exception {
        // given
        var info = save("Backup done", AlertSeverity.INFO, 2, AlertStatus.OPEN);
        var olderCritical = save("Disk almost full", AlertSeverity.CRITICAL, 0, AlertStatus.OPEN);
        var warning = save("Mail undeliverable", AlertSeverity.WARNING, 1, AlertStatus.ACKNOWLEDGED);
        var newerCritical = save("Disk full", AlertSeverity.CRITICAL, 0, AlertStatus.RESOLVED);

        // when / then
        mockMvc.perform(get("/alerts").param("page", "0").param("size", "10").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(newerCritical.id().toString(),
                        olderCritical.id().toString(), warning.id().toString(), info.id().toString())))
                .andExpect(jsonPath("$.items[0].title").value("Disk full"))
                .andExpect(jsonPath("$.items[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.items[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$.items[0].affectedSystem").value("nas01"))
                .andExpect(jsonPath("$.items[0].createdDate").isNotEmpty())
                .andExpect(jsonPath("$.totalCount").value(4));
    }

    @Test
    void thatListingAlertsByStatusesShouldResultIntoTheMatchingAlertsOnly() throws Exception {
        // given
        var open = save("Disk almost full", AlertSeverity.CRITICAL, 0, AlertStatus.OPEN);
        var acknowledged = save("Mail undeliverable", AlertSeverity.WARNING, 1, AlertStatus.ACKNOWLEDGED);
        save("Disk full", AlertSeverity.CRITICAL, 0, AlertStatus.RESOLVED);

        // when / then
        mockMvc.perform(get("/alerts").param("status", "OPEN", "ACKNOWLEDGED").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(open.id().toString(), acknowledged.id().toString())))
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    void thatListingAlertsWithAnUnknownStatusShouldResultIntoABadRequest() throws Exception {
        // when / then
        mockMvc.perform(get("/alerts").param("status", "UNKNOWN").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void thatListingAlertsWithoutTokenShouldResultIntoUnauthorized() throws Exception {
        // when / then
        mockMvc.perform(get("/alerts")).andExpect(status().isUnauthorized());
    }

    @Test
    void thatGettingAnAlertShouldResultIntoItsDataAndAudit() throws Exception {
        // given
        var alert = save("Disk almost full", AlertSeverity.CRITICAL, 0, AlertStatus.OPEN);

        // when / then
        mockMvc.perform(get("/alerts/{id}", alert.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Disk almost full"))
                .andExpect(jsonPath("$.data.message").value("Volume 1 is 94 % full"))
                .andExpect(jsonPath("$.data.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.affectedSystem").value("nas01"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.acknowledgedDate").doesNotExist())
                .andExpect(jsonPath("$.data.source").value("CORE"))
                .andExpect(jsonPath("$.data.sourceId").value(alert.sourceId().toString()))
                .andExpect(jsonPath("$.data.severityRank").doesNotExist())
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatGettingAnUnknownAlertShouldResultIntoNotFound() throws Exception {
        // when / then
        mockMvc.perform(get("/alerts/{id}", UUID.randomUUID()).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void thatAcknowledgingAnOpenAlertShouldResultIntoAnAcknowledgedAlertWithDate() throws Exception {
        // given
        var alert = save("Disk almost full", AlertSeverity.CRITICAL, 0, AlertStatus.OPEN);

        // when / then
        mockMvc.perform(post("/alerts/{id}/acknowledge", alert.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.data.acknowledgedDate").isNotEmpty())
                .andExpect(jsonPath("$.audit.version").value(1));
    }

    @Test
    void thatAcknowledgingAnAcknowledgedAlertShouldResultIntoAnUnchangedAlert() throws Exception {
        // given
        var alert = save("Mail undeliverable", AlertSeverity.WARNING, 1, AlertStatus.ACKNOWLEDGED);

        // when / then
        mockMvc.perform(post("/alerts/{id}/acknowledge", alert.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatAcknowledgingAResolvedAlertShouldResultIntoAConflict() throws Exception {
        // given
        var alert = save("Disk full", AlertSeverity.CRITICAL, 0, AlertStatus.RESOLVED);

        // when / then
        mockMvc.perform(post("/alerts/{id}/acknowledge", alert.id()).with(jwt())).andExpect(status().isConflict());
    }

    @Test
    void thatResolvingAnAcknowledgedAlertShouldResultIntoAResolvedAlertWithDate() throws Exception {
        // given
        var alert = save("Mail undeliverable", AlertSeverity.WARNING, 1, AlertStatus.ACKNOWLEDGED);

        // when / then
        mockMvc.perform(post("/alerts/{id}/resolve", alert.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedDate").isNotEmpty())
                .andExpect(jsonPath("$.audit.version").value(1));
    }

    @Test
    void thatResolvingAResolvedAlertShouldResultIntoAnUnchangedAlert() throws Exception {
        // given
        var alert = save("Disk full", AlertSeverity.CRITICAL, 0, AlertStatus.RESOLVED);

        // when / then
        mockMvc.perform(post("/alerts/{id}/resolve", alert.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatReopeningAResolvedAlertShouldResultIntoAnOpenAlertWithoutDates() throws Exception {
        // given
        var alert = alertRepository.save(save("Disk full", AlertSeverity.CRITICAL, 0, AlertStatus.RESOLVED)
                .toBuilder().acknowledgedDate(EARLIER).resolvedDate(EARLIER).build());

        // when / then
        mockMvc.perform(post("/alerts/{id}/reopen", alert.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.acknowledgedDate").doesNotExist())
                .andExpect(jsonPath("$.data.resolvedDate").doesNotExist())
                .andExpect(jsonPath("$.audit.version").value(2));
    }

    @Test
    void thatReopeningAnOpenAlertShouldResultIntoAnUnchangedAlert() throws Exception {
        // given
        var alert = save("Disk almost full", AlertSeverity.CRITICAL, 0, AlertStatus.OPEN);

        // when / then
        mockMvc.perform(post("/alerts/{id}/reopen", alert.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    private Alert save(String title, AlertSeverity severity, int severityRank, AlertStatus status) {
        return alertRepository.save(Alert.builder()
                .source(AlertSource.CORE)
                .sourceId(UUID.randomUUID())
                .title(title)
                .message("Volume 1 is 94 % full")
                .severity(severity)
                .severityRank(severityRank)
                .affectedSystem("nas01")
                .status(status)
                .build());
    }
}
