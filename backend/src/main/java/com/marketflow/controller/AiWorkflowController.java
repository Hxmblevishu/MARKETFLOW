package com.marketflow.controller;

import com.marketflow.dto.AiGenerateWorkflowRequest;
import com.marketflow.dto.AiGenerateWorkflowResponse;
import com.marketflow.service.AiWorkflowGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI Generation", description = "AI-powered natural language workflow generation")
public class AiWorkflowController {

    private final AiWorkflowGeneratorService generatorService;

    public AiWorkflowController(AiWorkflowGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping("/generate-workflow")
    @Operation(summary = "Generate workflow from prompt", description = "Generates a fully connected React Flow DAG from natural language")
    public ResponseEntity<AiGenerateWorkflowResponse> generateWorkflow(@Valid @RequestBody AiGenerateWorkflowRequest request) {
        AiGenerateWorkflowResponse response = generatorService.generateWorkflow(request);
        return ResponseEntity.ok(response);
    }
}
