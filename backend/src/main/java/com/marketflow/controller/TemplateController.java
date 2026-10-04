package com.marketflow.controller;

import com.marketflow.dto.WorkflowDetailResponse;
import com.marketflow.dto.WorkflowTemplateDto;
import com.marketflow.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/templates")
@Tag(name = "Templates", description = "Pre-built Marketing Automation Workflow Templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "List workflow templates", description = "Returns available marketing workflow templates, optionally filtered by category")
    public ResponseEntity<List<WorkflowTemplateDto>> listTemplates(
            @RequestParam(name = "category", required = false) String category) {
        return ResponseEntity.ok(templateService.listTemplates(category));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template details", description = "Returns a single template's pre-configured nodes and edges")
    public ResponseEntity<WorkflowTemplateDto> getTemplate(@PathVariable String id) {
        return ResponseEntity.ok(templateService.getTemplate(id));
    }

    @PostMapping("/{id}/instantiate")
    @Operation(summary = "Instantiate template", description = "Clones a pre-built template into an active user workflow")
    public ResponseEntity<WorkflowDetailResponse> instantiateTemplate(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String name = (body != null) ? body.get("name") : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.instantiateTemplate(id, name));
    }
}
