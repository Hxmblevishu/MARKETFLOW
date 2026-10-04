package com.marketflow.engine.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.engine.JsonPathExpressionResolver;

/**
 * Backward compatibility alias for {@link ActionHandler}.
 * @deprecated Use {@link ActionHandler} instead.
 */
@Deprecated
public class ActionNodeExecutor extends ActionHandler {

    public ActionNodeExecutor(JsonPathExpressionResolver expressionResolver, ObjectMapper objectMapper) {
        super(expressionResolver, objectMapper);
    }
}
