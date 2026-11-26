package de.kirchrath.luna.appointment.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.appointment.config.TestcontainersConfig;
import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.data.entity.AppointmentSource;
import de.kirchrath.luna.appointment.data.entity.AppointmentStatus;
import de.kirchrath.luna.appointment.data.repository.AppointmentRepository;
import de.kirchrath.luna.appointment.data.repository.CoreAppointmentProjectionRepository;
import de.kirchrath.luna.appointment.services.TimeProvider;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AppointmentControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-26T12:00:00+01:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CoreAppointmentProjectionRepository projectionRepository;

    @MockitoSpyBean
    private TimeProvider timeProvider;

    private Appointment yesterday;
    private Appointment running;
    private Appointment tomorrow;
    private Appointment nextWeek;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        projectionRepository.deleteAll();
        when(timeProvider.now()).thenReturn(NOW);
        yesterday = save("Dentist", "2026-11-25T10:00:00+01:00", "2026-11-25T11:00:00+01:00");
        running = save("Conference", "2026-11-26T09:00:00+01:00", "2026-11-26T17:00:00+01:00");
        tomorrow = save("Bridge day", "2026-11-27T00:00:00+01:00", null);
        nextWeek = save("Tyre change", "2026-12-03T09:00:00+01:00", null);
    }

    @Test
    void thatListingUpcomingAppointmentsShouldResultIntoRunningAndFutureAppointmentsEarliestFirst() throws Exception {
        // when / then
        mockMvc.perform(get("/appointments").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(
                        running.id().toString(), tomorrow.id().toString(), nextWeek.id().toString())))
                .andExpect(jsonPath("$.items[0].title").value("Conference"))
                .andExpect(jsonPath("$.items[0].end").isNotEmpty())
                .andExpect(jsonPath("$.items[0].allDay").value(false))
                .andExpect(jsonPath("$.items[0].location").value("Town hall"))
                .andExpect(jsonPath("$.items[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.items[0].edited").value(false))
                .andExpect(jsonPath("$.items[2].end").doesNotExist())
                .andExpect(jsonPath("$.totalCount").value(3));
    }

    @Test
    void thatListingPastAppointmentsShouldResultIntoFinishedAppointmentsLatestFirst() throws Exception {
        // when / then
        mockMvc.perform(get("/appointments").param("period", "PAST").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(yesterday.id().toString())));
    }

    @Test
    void thatListingAllAppointmentsShouldResultIntoAllAppointmentsEarliestFirst() throws Exception {
        // when / then
        mockMvc.perform(get("/appointments").param("period", "ALL").param("page", "0").param("size", "2").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(yesterday.id().toString(), running.id().toString())))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalCount").value(4));
    }

    @Test
    void thatListingAppointmentsInARangeShouldResultIntoTheOverlappingAppointments() throws Exception {
        // given
        var allDayTomorrow = appointmentRepository.save(tomorrow.toBuilder()
                .allDay(true)
                .effectiveEnd(OffsetDateTime.parse("2026-11-28T00:00:00+01:00"))
                .build());

        // when / then
        mockMvc.perform(get("/appointments/range")
                        .param("from", "2026-11-26T12:00:00+01:00")
                        .param("to", "2026-11-28T00:00:00+01:00")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(running.id().toString(), allDayTomorrow.id().toString())))
                .andExpect(jsonPath("$.items[1].allDay").value(true))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    void thatListingAppointmentsInARangeWithoutEndShouldResultIntoABadRequest() throws Exception {
        // when / then
        mockMvc.perform(get("/appointments/range").param("from", "2026-11-26T12:00:00+01:00").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thatListingAppointmentsWithoutTokenShouldResultIntoUnauthorized() throws Exception {
        // when / then
        mockMvc.perform(get("/appointments")).andExpect(status().isUnauthorized());
    }

    @Test
    void thatGettingAnAppointmentShouldResultIntoItsDataAndAudit() throws Exception {
        // when / then
        mockMvc.perform(get("/appointments/{id}", running.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Conference"))
                .andExpect(jsonPath("$.data.allDay").value(false))
                .andExpect(jsonPath("$.data.location").value("Town hall"))
                .andExpect(jsonPath("$.data.description").value("Bring the badge"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.cancelledDate").doesNotExist())
                .andExpect(content().string(not(containsString(":null"))))
                .andExpect(jsonPath("$.data.effectiveEnd").doesNotExist())
                .andExpect(jsonPath("$.data.source").value("CORE"))
                .andExpect(jsonPath("$.data.edited").value(false))
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatGettingAnUnknownAppointmentShouldResultIntoNotFound() throws Exception {
        // when / then
        mockMvc.perform(get("/appointments/{id}", UUID.randomUUID()).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void thatUpdatingAnAppointmentToAllDayShouldResultIntoAnEditedAppointmentOverUntilTheEndOfItsLastDay()
            throws Exception {
        // when / then
        mockMvc.perform(put("/appointments/{id}", nextWeek.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Holiday", "start": "2026-12-03T00:00:00+01:00",
                                 "end": "2026-12-05T00:00:00+01:00", "allDay": true, "version": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Holiday"))
                .andExpect(jsonPath("$.data.allDay").value(true))
                .andExpect(jsonPath("$.data.location").doesNotExist())
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.audit.version").value(1));
        assertThat(appointmentRepository.findById(nextWeek.id())).get()
                .extracting(Appointment::effectiveEnd)
                .satisfies(end -> assertThat(end.toInstant())
                        .isEqualTo(OffsetDateTime.parse("2026-12-06T00:00:00+01:00").toInstant()));
    }

    @Test
    void thatUpdatingAnAppointmentWithoutEndShouldResultIntoAnAppointmentOverAtItsStart() throws Exception {
        // when / then
        mockMvc.perform(put("/appointments/{id}", nextWeek.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Tyre change", "start": "2026-12-04T09:00:00+01:00", "allDay": false,
                                 "location": "Garage Lenz", "version": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.end").doesNotExist())
                .andExpect(jsonPath("$.data.location").value("Garage Lenz"));
        assertThat(appointmentRepository.findById(nextWeek.id())).get()
                .extracting(Appointment::effectiveEnd)
                .satisfies(end -> assertThat(end.toInstant())
                        .isEqualTo(OffsetDateTime.parse("2026-12-04T09:00:00+01:00").toInstant()));
    }

    @Test
    void thatUpdatingAnAppointmentWithEndBeforeStartShouldResultIntoABadRequest() throws Exception {
        // when / then
        mockMvc.perform(put("/appointments/{id}", nextWeek.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Tyre change", "start": "2026-12-03T09:00:00+01:00",
                                 "end": "2026-12-03T08:00:00+01:00", "allDay": false, "version": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thatUpdatingAnAppointmentInAnOutdatedVersionShouldResultIntoAConflict() throws Exception {
        // when / then
        mockMvc.perform(put("/appointments/{id}", nextWeek.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Tyre change", "start": "2026-12-03T09:00:00+01:00", "allDay": false,
                                 "version": 3}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void thatCancellingAScheduledAppointmentShouldResultIntoACancelledAppointmentWithDate() throws Exception {
        // when / then
        mockMvc.perform(post("/appointments/{id}/cancel", tomorrow.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledDate").isNotEmpty())
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.audit.version").value(1));

        // when / then
        mockMvc.perform(post("/appointments/{id}/cancel", tomorrow.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.version").value(1));
    }

    @Test
    void thatRestoringACancelledAppointmentShouldResultIntoAScheduledAppointmentWithoutDate() throws Exception {
        // given
        appointmentRepository.save(tomorrow.toBuilder().status(AppointmentStatus.CANCELLED).cancelledDate(NOW).build());

        // when / then
        mockMvc.perform(post("/appointments/{id}/restore", tomorrow.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.cancelledDate").doesNotExist())
                .andExpect(jsonPath("$.audit.version").value(2));

        // when / then
        mockMvc.perform(post("/appointments/{id}/restore", tomorrow.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.version").value(2));
    }

    private Appointment save(String title, String start, String end) {
        var startTime = OffsetDateTime.parse(start);
        var endTime = end == null ? null : OffsetDateTime.parse(end);
        return appointmentRepository.save(Appointment.builder()
                .source(AppointmentSource.CORE)
                .sourceId(UUID.randomUUID())
                .title(title)
                .start(startTime)
                .end(endTime)
                .location("Town hall")
                .description("Bring the badge")
                .effectiveEnd(endTime == null ? startTime : endTime)
                .status(AppointmentStatus.SCHEDULED)
                .build());
    }
}
