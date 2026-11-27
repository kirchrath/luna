package de.kirchrath.luna.appointment.inbound;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.kirchrath.luna.appointment.dto.AppointmentDto;
import de.kirchrath.luna.appointment.dto.AppointmentListDto;
import de.kirchrath.luna.appointment.dto.AppointmentPeriodDto;
import de.kirchrath.luna.appointment.dto.AppointmentUpdateDto;
import de.kirchrath.luna.appointment.dto.LifeAreaDto;
import de.kirchrath.luna.appointment.services.AppointmentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointments")
public class AppointmentController {

    private static final String ID = "/{id}";

    private final AppointmentApplicationService appointmentApplicationService;

    public AppointmentController(AppointmentApplicationService appointmentApplicationService) {
        this.appointmentApplicationService = appointmentApplicationService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listAppointments",
            parameters = {
                @Parameter(name = "page", in = ParameterIn.QUERY,
                        schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
                @Parameter(name = "size", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "1"))
            },
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "400", content = @Content),
                @ApiResponse(responseCode = "401", content = @Content)
            })
    public AppointmentListDto listAppointments(
            @RequestParam(defaultValue = "UPCOMING") AppointmentPeriodDto period,
            @RequestParam(required = false) LifeAreaDto area,
            @Parameter(hidden = true) Pageable pageable) {
        return appointmentApplicationService.listAppointments(period, area, pageable);
    }

    @GetMapping(path = "/range", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listAppointmentsInRange",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "400", content = @Content),
                @ApiResponse(responseCode = "401", content = @Content)
            })
    public AppointmentListDto listAppointmentsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) LifeAreaDto area) {
        return appointmentApplicationService.listAppointmentsInRange(from, to, area);
    }

    @GetMapping(path = ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "getAppointment",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public AppointmentDto getAppointment(@PathVariable UUID id) {
        return appointmentApplicationService.getAppointment(id);
    }

    @PutMapping(path = ID, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "updateAppointment",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "400", content = @Content),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content),
                @ApiResponse(responseCode = "409", content = @Content)
            })
    public AppointmentDto updateAppointment(@PathVariable UUID id, @Valid @RequestBody AppointmentUpdateDto update) {
        return appointmentApplicationService.updateAppointment(id, update);
    }

    @PostMapping(path = ID + "/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "cancelAppointment",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public AppointmentDto cancelAppointment(@PathVariable UUID id) {
        return appointmentApplicationService.cancelAppointment(id);
    }

    @PostMapping(path = ID + "/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "restoreAppointment",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public AppointmentDto restoreAppointment(@PathVariable UUID id) {
        return appointmentApplicationService.restoreAppointment(id);
    }
}
