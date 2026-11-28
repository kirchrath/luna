package de.kirchrath.luna.task.inbound;

import java.util.List;
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

import de.kirchrath.luna.task.dto.LifeAreaDto;
import de.kirchrath.luna.task.dto.TaskDto;
import de.kirchrath.luna.task.dto.TaskListDto;
import de.kirchrath.luna.task.dto.TaskStatusDto;
import de.kirchrath.luna.task.dto.TaskUpdateDto;
import de.kirchrath.luna.task.services.TaskApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks")
public class TaskController {


    private static final String ID = "/{id}";
    private static final String AREA = "area";

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listTasks",
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
    public TaskListDto listTasks(
            @RequestParam(required = false) TaskStatusDto status,
            @RequestParam(name = AREA, required = false, defaultValue = "") List<LifeAreaDto> areas,
            @Parameter(hidden = true) Pageable pageable) {
        return taskApplicationService.listTasks(status, areas, pageable);
    }

    @GetMapping(path = ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "getTask",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public TaskDto getTask(@PathVariable UUID id) {
        return taskApplicationService.getTask(id);
    }

    @PutMapping(path = ID, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "updateTask",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "400", content = @Content),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content),
                @ApiResponse(responseCode = "409", content = @Content)
            })
    public TaskDto updateTask(@PathVariable UUID id, @Valid @RequestBody TaskUpdateDto update) {
        return taskApplicationService.updateTask(id, update);
    }

    @PostMapping(path = ID + "/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "completeTask",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public TaskDto completeTask(@PathVariable UUID id) {
        return taskApplicationService.completeTask(id);
    }

    @PostMapping(path = ID + "/reopen", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "reopenTask",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(responseCode = "401", content = @Content),
                @ApiResponse(responseCode = "404", content = @Content)
            })
    public TaskDto reopenTask(@PathVariable UUID id) {
        return taskApplicationService.reopenTask(id);
    }
}
