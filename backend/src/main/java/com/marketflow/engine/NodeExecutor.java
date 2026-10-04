package com.marketflow.engine;

import com.marketflow.dto.NodeDto;

public interface NodeExecutor {

    boolean supports(String nodeType);

    NodeExecutionResult execute(NodeDto node, ExecutionContext context);
}
