package com.marketflow.config;

import com.marketflow.dto.ApiErrorResponse;
import com.marketflow.exception.ExecutionNotFoundException;
import com.marketflow.exception.InvalidWorkflowGraphException;
import com.marketflow.exception.WorkflowNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 with WORKFLOW_NOT_FOUND error code")
    void testHandleWorkflowNotFound() {
        WorkflowNotFoundException ex = new WorkflowNotFoundException("wf_999");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleWorkflowNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("WORKFLOW_NOT_FOUND", response.getBody().getError().getCode());
        assertEquals("Workflow does not exist.", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("Should return 404 with EXECUTION_NOT_FOUND error code")
    void testHandleExecutionNotFound() {
        ExecutionNotFoundException ex = new ExecutionNotFoundException("exec_999");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleExecutionNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EXECUTION_NOT_FOUND", response.getBody().getError().getCode());
        assertEquals("Execution does not exist.", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("Should return 400 with dynamic error code for InvalidWorkflowGraphException")
    void testHandleInvalidWorkflowGraph() {
        InvalidWorkflowGraphException ex = new InvalidWorkflowGraphException("CYCLE_DETECTED", "Circular reference found");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleInvalidWorkflowGraph(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CYCLE_DETECTED", response.getBody().getError().getCode());
        assertEquals("Circular reference found", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("Should return 400 with INVALID_ARGUMENT for IllegalArgumentException")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input parameter");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().getError().getCode());
        assertEquals("Bad input parameter", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("Should return 500 with INTERNAL_SERVER_ERROR for unexpected exceptions")
    void testHandleGeneralException() {
        RuntimeException ex = new RuntimeException("Database timeout");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError().getCode());
        assertEquals("Database timeout", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("Should return 404 with TEMPLATE_NOT_FOUND for TemplateNotFoundException")
    void testHandleTemplateNotFound() {
        com.marketflow.exception.TemplateNotFoundException ex = new com.marketflow.exception.TemplateNotFoundException("tpl_123");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleTemplateNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TEMPLATE_NOT_FOUND", response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("tpl_123"));
    }

    @Test
    @DisplayName("Should return 409 with DUPLICATE_RESOURCE for DuplicateResourceException")
    void testHandleDuplicateResource() {
        com.marketflow.exception.DuplicateResourceException ex = new com.marketflow.exception.DuplicateResourceException("Workflow", "Instagram Triage");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDuplicateResource(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DUPLICATE_RESOURCE", response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("Instagram Triage"));
    }

    @Test
    @DisplayName("Should return 422 with NODE_EXECUTION_FAILED for NodeExecutionException")
    void testHandleNodeExecutionException() {
        com.marketflow.exception.NodeExecutionException ex = new com.marketflow.exception.NodeExecutionException("node_http_1", "action_http", "Connection refused");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNodeExecutionException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NODE_EXECUTION_FAILED", response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("node_http_1"));
    }

    @Test
    @DisplayName("Should return 502 with AI_SERVICE_ERROR for AiServiceException")
    void testHandleAiServiceException() {
        com.marketflow.exception.AiServiceException ex = new com.marketflow.exception.AiServiceException("OpenAI", "Rate limit exceeded (429)");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAiServiceException(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AI_SERVICE_ERROR", response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("OpenAI"));
        assertTrue(response.getBody().getError().getMessage().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("Should return 409 with WORKFLOW_STATE_CONFLICT for IllegalStateException")
    void testHandleIllegalStateException() {
        IllegalStateException ex = new IllegalStateException("Cannot execute a paused workflow: wf_100");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalStateException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("WORKFLOW_STATE_CONFLICT", response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("Cannot execute a paused workflow"));
    }

    @Test
    @DisplayName("Should return 409 with DATABASE_CONFLICT for DataIntegrityViolationException")
    void testHandleDataIntegrityViolation() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException("Unique constraint violation: idx_workflows_name");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATABASE_CONFLICT", response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("Database constraint violation"));
    }
}
