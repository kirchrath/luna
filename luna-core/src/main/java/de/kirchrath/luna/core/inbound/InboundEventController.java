package de.kirchrath.luna.core.inbound;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import de.kirchrath.luna.core.services.InboundEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/inbound-events")
@Tag(name = "Inbound events", description = "Events that luna-core receives and analyses")
public class InboundEventController {

    private final InboundEventService inboundEventService;

    public InboundEventController(InboundEventService inboundEventService) {
        this.inboundEventService = inboundEventService;
    }

    @PostMapping(
            path = "/manual",
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
        return inboundEventService.createManualInboundEvent(input);
    }
}
