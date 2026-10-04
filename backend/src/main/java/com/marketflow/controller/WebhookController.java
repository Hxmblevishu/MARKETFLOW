package com.marketflow.controller;

import com.marketflow.dto.ExecuteWorkflowRequest;
import com.marketflow.dto.ExecuteWorkflowResponse;
import com.marketflow.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Inbound Webhook Ingestion Gateway")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final ExecutionService executionService;

    public WebhookController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/{workflowId}")
    @Operation(summary = "Ingest webhook event", description = "Public webhook endpoint that receives JSON events and triggers workflow execution")
    public ResponseEntity<ExecuteWorkflowResponse> ingestWebhook(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {

        log.info("Received inbound webhook for workflow [{}]", workflowId);

        Map<String, Object> input = new LinkedHashMap<>();
        if (payload != null) {
            input.putAll(payload);
        }
        input.put("webhookHeaders", headers);
        input.put("webhookReceivedAt", Instant.now().toString());

        ExecuteWorkflowResponse response = executionService.executeWorkflow(workflowId, new ExecuteWorkflowRequest(input));
        return ResponseEntity.ok(response);
    }
}
