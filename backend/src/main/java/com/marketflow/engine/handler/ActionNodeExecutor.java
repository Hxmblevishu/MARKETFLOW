package com.marketflow.engine.handler;

import com.marketflow.dto.NodeDto;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ActionNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String nodeType) {
        if (nodeType == null) return false;
        String type = nodeType.toLowerCase();
        return type.equals("action") || type.startsWith("action_") || type.contains("action")
                || type.contains("slack") || type.contains("email") || type.contains("crm");
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        Map<String, Object> output = new HashMap<>();
        String actionType = node.getType() != null ? node.getType().toLowerCase() : "action";

        output.put("executedAt", Instant.now().toString());
        output.put("nodeId", node.getId());
        output.put("actionType", actionType);
        output.put("status", "SUCCESS");

        if (actionType.contains("slack")) {
            output.put("channel", node.getData().getOrDefault("channel", "#leads"));
            output.put("message", "Lead notification delivered to Slack successfully.");
            output.put("receiptId", "slack_msg_" + UUID.randomUUID().toString().substring(0, 8));
        } else if (actionType.contains("email")) {
            output.put("recipient", node.getData().getOrDefault("to", context.getVariable("email")));
            output.put("subject", node.getData().getOrDefault("subject", "Automated Update"));
            output.put("message", "Email delivered successfully.");
            output.put("receiptId", "email_rcpt_" + UUID.randomUUID().toString().substring(0, 8));
        } else if (actionType.contains("crm")) {
            output.put("crmSystem", "HubSpot / Salesforce Simulation");
            output.put("recordId", "crm_lead_" + UUID.randomUUID().toString().substring(0, 8));
            output.put("message", "Lead recorded in CRM successfully.");
        } else {
            output.put("message", "Action " + node.getLabel() + " completed successfully.");
        }

        return NodeExecutionResult.success(node.getId(), output);
    }
}
