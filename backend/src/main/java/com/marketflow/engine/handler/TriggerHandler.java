package com.marketflow.engine.handler;

import com.marketflow.dto.NodeDto;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.NodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Phase 6: TriggerHandler
 * Ingests and normalizes triggers for marketing workflows:
 * - manual_trigger: direct payload from user interface or test execution
 * - webhook_trigger: incoming HTTP webhook payloads with headers and query parameters
 * - scheduled_trigger: cron or interval-based scheduled marketing campaigns
 */
@Component
public class TriggerHandler implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(TriggerHandler.class);

    @Override
    public boolean supports(String nodeType) {
        if (nodeType == null) return false;
        String type = nodeType.toLowerCase();
        return type.equals("trigger") || type.startsWith("trigger_") || type.contains("trigger")
                || type.contains("webhook") || type.contains("schedule");
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        Map<String, Object> data = node.getData() != null ? node.getData() : Collections.emptyMap();
        String nodeType = node.getType() != null ? node.getType().toLowerCase() : "trigger";
        String triggerType = determineTriggerType(nodeType, data);

        Map<String, Object> triggerPayload = context.getTriggerPayload();
        Map<String, Object> output = new LinkedHashMap<>();

        output.put("nodeId", node.getId());
        output.put("nodeLabel", node.getLabel());
        output.put("triggerType", triggerType);
        output.put("triggeredAt", Instant.now().toString());

        switch (triggerType) {
            case "webhook_trigger" -> {
                String webhookId = data.containsKey("webhookId") ? data.get("webhookId").toString()
                        : "wh_" + UUID.randomUUID().toString().substring(0, 8);
                Map<String, Object> headers = data.containsKey("headers") && data.get("headers") instanceof Map<?, ?> m
                        ? castMap(m)
                        : Map.of("content-type", "application/json", "user-agent", "Marketflow-Webhook/1.0");

                output.put("webhookId", webhookId);
                output.put("headers", headers);
                output.put("triggerSource", "HTTP_POST");
                output.put("source", "HTTP_POST");
                output.put("status", "RECEIVED");
                output.put("payload", triggerPayload);
            }
            case "scheduled_trigger" -> {
                String cron = data.getOrDefault("cron", "0 0 * * *").toString();
                String timezone = data.getOrDefault("timezone", "UTC").toString();

                output.put("cron", cron);
                output.put("timezone", timezone);
                output.put("triggerSource", "SCHEDULER");
                output.put("source", "SCHEDULER");
                output.put("status", "DISPATCHED");
                output.put("payload", triggerPayload);
            }
            default -> { // manual_trigger
                output.put("triggerSource", "MANUAL_UI");
                output.put("source", "MANUAL_UI");
                output.put("status", "TRIGGERED");
                output.put("payload", triggerPayload);
            }
        }

        // Spread trigger payload fields to the root of the output map
        // User payload MUST take precedence over default trigger metadata!
        if (triggerPayload != null) {
            for (Map.Entry<String, Object> entry : triggerPayload.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    output.put(entry.getKey(), entry.getValue());
                }
            }
        }

        log.info("Trigger node [{}] executed successfully. Subtype: [{}], fields: {}",
                node.getId(), triggerType, triggerPayload != null ? triggerPayload.size() : 0);

        return NodeExecutionResult.success(node.getId(), output);
    }

    private String determineTriggerType(String nodeType, Map<String, Object> data) {
        if (data.containsKey("triggerType") && data.get("triggerType") != null) {
            return data.get("triggerType").toString().toLowerCase();
        }
        if (nodeType.contains("webhook")) {
            return "webhook_trigger";
        }
        if (nodeType.contains("schedule") || nodeType.contains("cron")) {
            return "scheduled_trigger";
        }
        return "manual_trigger";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                map.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return map;
    }
}
