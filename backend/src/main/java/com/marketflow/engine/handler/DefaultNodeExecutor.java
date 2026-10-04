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
public class DefaultNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String nodeType) {
        // Fallback handler supports all node types
        return true;
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        Map<String, Object> output = new HashMap<>();
        output.put("nodeId", node.getId());
        output.put("type", node.getType());
        output.put("label", node.getLabel());
        output.put("executedAt", Instant.now().toString());
        output.put("message", "Executed node " + node.getId() + " (" + node.getLabel() + ")");
        return NodeExecutionResult.success(node.getId(), output);
    }
}
