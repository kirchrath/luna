package de.kirchrath.luna.alert.inbound;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.kirchrath.luna.alert.dto.AlertDto;
import de.kirchrath.luna.alert.dto.AlertListDto;
import de.kirchrath.luna.alert.dto.AlertStatusDto;
import de.kirchrath.luna.alert.services.AlertApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/alerts")
@Tag(name = "Alerts")
public class AlertController {

    private static final String ID = "/{id}";

    private final AlertApplicationService alertApplicationService;

    public AlertController(AlertApplicationService alertApplicationService) {
        this.alertApplicationService = alertApplicationService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listAlerts",
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
    public AlertListDto listAlerts(
            @RequestParam(required = false, defaultValue = "") List<AlertStatusDto> status,
            @Parameter(hidden = true) Pageable pageable) {
        return alertApplicationService.listAlerts(status, pageable);
    }

    @GetMapping(path = ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "getAlert",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public AlertDto getAlert(@PathVariable UUID id) {
        return alertApplicationService.getAlert(id);
    }

    @PostMapping(path = ID + "/acknowledge", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "acknowledgeAlert",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content),
                @ApiResponse(responseCode = "409", content = @Content)
            })
    public AlertDto acknowledgeAlert(@PathVariable UUID id) {
        return alertApplicationService.acknowledgeAlert(id);
    }

    @PostMapping(path = ID + "/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "resolveAlert",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public AlertDto resolveAlert(@PathVariable UUID id) {
        return alertApplicationService.resolveAlert(id);
    }

    @PostMapping(path = ID + "/reopen", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "reopenAlert",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public AlertDto reopenAlert(@PathVariable UUID id) {
        return alertApplicationService.reopenAlert(id);
    }
}
