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
@Tag(name = "Inbound events")
public class InboundEventController {

    private static final String MANUAL = "/manual";

    private final InboundEventApplicationService inboundEventApplicationService;

    public InboundEventController(InboundEventApplicationService inboundEventApplicationService) {
        this.inboundEventApplicationService = inboundEventApplicationService;
    }

    @GetMapping(path = MANUAL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listManualInboundEvents",
            parameters = {
                @Parameter(name = "page", in = ParameterIn.QUERY,
                        schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
                @Parameter(name = "size", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "1"))
            },
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content)
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
            responses = {
                @ApiResponse(responseCode = "202"),
                @ApiResponse(responseCode = "400", content = @Content),
                @ApiResponse(responseCode = "401", content = @Content)
            })
    public ResourceReferenceDto createManualInboundEvent(@Valid @RequestBody ManualInputDataDto input) {
        return inboundEventApplicationService.createManualInboundEvent(input);
    }
}
