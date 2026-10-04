package com.marketflow.engine.handler;

import com.marketflow.dto.NodeDto;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class TriggerNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String nodeType) {
        if (nodeType == null) return false;
        String type = nodeType.toLowerCase();
        return type.equals("trigger") || type.startsWith("trigger_") || type.contains("trigger");
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        Map<String, Object> output = new HashMap<>();
        output.put("triggeredAt", Instant.now().toString());
        output.put("nodeId", node.getId());
        output.put("nodeLabel", node.getLabel());
        output.put("payload", context.getTriggerPayload());
        return NodeExecutionResult.success(node.getId(), output);
    }
}
