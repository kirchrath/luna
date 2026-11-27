package de.kirchrath.luna.note.inbound;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.kirchrath.luna.note.dto.LifeAreaDto;
import de.kirchrath.luna.note.dto.NoteDto;
import de.kirchrath.luna.note.dto.NoteListDto;
import de.kirchrath.luna.note.dto.NoteStatusDto;
import de.kirchrath.luna.note.dto.NoteUpdateDto;
import de.kirchrath.luna.note.services.NoteApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/notes")
@Tag(name = "Notes")
public class NoteController {

    private static final String ID = "/{id}";

    private final NoteApplicationService noteApplicationService;

    public NoteController(NoteApplicationService noteApplicationService) {
        this.noteApplicationService = noteApplicationService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listNotes",
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
    public NoteListDto listNotes(
            @RequestParam(required = false) NoteStatusDto status,
            @RequestParam(required = false) LifeAreaDto area,
            @Parameter(hidden = true) Pageable pageable) {
        return noteApplicationService.listNotes(status, area, pageable);
    }

    @GetMapping(path = ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "getNote",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public NoteDto getNote(@PathVariable UUID id) {
        return noteApplicationService.getNote(id);
    }

    @PutMapping(path = ID, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "updateNote",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "400", content = @Content),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content),
                @ApiResponse(responseCode = "409", content = @Content)
            })
    public NoteDto updateNote(@PathVariable UUID id, @Valid @RequestBody NoteUpdateDto update) {
        return noteApplicationService.updateNote(id, update);
    }

    @PostMapping(path = ID + "/archive", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "archiveNote",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public NoteDto archiveNote(@PathVariable UUID id) {
        return noteApplicationService.archiveNote(id);
    }

    @PostMapping(path = ID + "/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "restoreNote",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public NoteDto restoreNote(@PathVariable UUID id) {
        return noteApplicationService.restoreNote(id);
    }
}
