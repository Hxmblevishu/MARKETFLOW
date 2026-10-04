package com.marketflow.engine.handler;

import com.marketflow.engine.JsonPathExpressionResolver;

/**
 * Backward compatibility alias for {@link ConditionHandler}.
 * @deprecated Use {@link ConditionHandler} instead.
 */
@Deprecated
public class ConditionNodeExecutor extends ConditionHandler {

    public ConditionNodeExecutor(JsonPathExpressionResolver expressionResolver) {
        super(expressionResolver);
    }
}
