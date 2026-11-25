package de.kirchrath.luna.core.inbound;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.kirchrath.luna.core.dto.ManualInboundEventListDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import de.kirchrath.luna.core.services.InboundEventApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/inbound-events")
@Tag(name = "Inbound events", description = "Events that luna-core receives and analyses")
public class InboundEventController {

    private static final String MANUAL = "/manual";

    private final InboundEventApplicationService inboundEventApplicationService;

    public InboundEventController(InboundEventApplicationService inboundEventApplicationService) {
        this.inboundEventApplicationService = inboundEventApplicationService;
    }

    @GetMapping(path = MANUAL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listManualInboundEvents",
            summary = "List manual inputs with their processing status, newest first",
            parameters = {
                @Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number, starting at 0",
                        schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
                @Parameter(name = "size", in = ParameterIn.QUERY,
                        description = "Page size; default and maximum are configured in luna-core, "
                                + "larger values are limited to the maximum",
                        schema = @Schema(type = "integer", minimum = "1"))
            },
            responses = {
                @ApiResponse(responseCode = "200", description = "Page of manual inputs"),
                @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
            })
    public ManualInboundEventListDto listManualInboundEvents(@Parameter(hidden = true) Pageable pageable) {
        return inboundEventApplicationService.listManualInboundEvents(pageable);
    }

    @PostMapping(
            path = MANUAL,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            operationId = "createManualInboundEvent",
            summary = "Submit a manual input as inbound event",
            responses = {
                @ApiResponse(responseCode = "202", description = "Input accepted and stored for analysis"),
                @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
                @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
            })
    public ResourceReferenceDto createManualInboundEvent(@Valid @RequestBody ManualInputDataDto input) {
        return inboundEventApplicationService.createManualInboundEvent(input);
    }
}
