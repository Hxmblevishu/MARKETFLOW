package com.marketflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.*;
import com.marketflow.model.Workflow;
import com.marketflow.model.WorkflowTemplate;
import com.marketflow.model.enums.WorkflowStatus;
import com.marketflow.repository.WorkflowRepository;
import com.marketflow.repository.WorkflowTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(username = "judge@marketflow.demo", roles = {"USER"})
@DisplayName("Phase 8: REST API Controllers & Webhook Integration Tests")
class Phase8RestControllersTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    private Workflow sampleWorkflow;

    @BeforeEach
    void setUp() throws Exception {
        WorkflowGraphDto graph = new WorkflowGraphDto(
                List.of(
                        new NodeDto("trig_1", "manual_trigger", new PositionDto(100, 100), Map.of("label", "Manual Lead")),
                        new NodeDto("act_1", "action_email", new PositionDto(100, 250), Map.of("label", "Send Welcome", "recipient", "lead@company.com"))
                ),
                List.of(new EdgeDto("e1", "trig_1", "act_1"))
        );

        Workflow wf = new Workflow("Integration Test Flow", "Demo workflow for controller tests", objectMapper.writeValueAsString(graph));
        wf.setStatus(WorkflowStatus.ACTIVE);
        sampleWorkflow = workflowRepository.save(wf);
    }

    @Test
    @DisplayName("Ping: GET /api/ping returns UP status for Render keep-alive")
    void testKeepAlivePing() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.message", containsString("Render keep-alive ping acknowledged")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Metrics: GET /api/metrics returns aggregated execution and workflow statistics")
    void testMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkflows", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.successRatePercentage", notNullValue()));
    }

    @Test
    @DisplayName("Workflow CRUD: Create workflow validates graph and persists entity")
    void testCreateWorkflow() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
                "New Lead Workflow",
                "Automated qualification sequence",
                List.of(
                        new NodeDto("t_1", "webhook_trigger", new PositionDto(0, 0), Map.of("label", "Webhook")),
                        new NodeDto("a_1", "action_slack", new PositionDto(0, 100), Map.of("channel", "#leads"))
                ),
                List.of(new EdgeDto("e1", "t_1", "a_1"))
        );

        mockMvc.perform(post("/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("New Lead Workflow")))
                .andExpect(jsonPath("$.nodes", hasSize(2)))
                .andExpect(jsonPath("$.edges", hasSize(1)));
    }

    @Test
    @DisplayName("Workflow CRUD: Create workflow with multiple triggers rejects with 400 Bad Request")
    void testCreateWorkflowWithMultipleTriggersFails() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
                "Invalid Flow",
                "Has two triggers",
                List.of(
                        new NodeDto("t_1", "manual_trigger", new PositionDto(0, 0), Map.of()),
                        new NodeDto("t_2", "webhook_trigger", new PositionDto(0, 50), Map.of()),
                        new NodeDto("a_1", "action_email", new PositionDto(0, 100), Map.of())
                ),
                List.of(new EdgeDto("e1", "t_1", "a_1"), new EdgeDto("e2", "t_2", "a_1"))
        );

        mockMvc.perform(post("/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("MULTIPLE_TRIGGERS")));
    }

    @Test
    @DisplayName("Workflow CRUD: List and Get workflow by ID")
    void testListAndGetWorkflow() throws Exception {
        mockMvc.perform(get("/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflows", not(empty())));

        mockMvc.perform(get("/workflows/" + sampleWorkflow.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sampleWorkflow.getId())))
                .andExpect(jsonPath("$.name", is(sampleWorkflow.getName())))
                .andExpect(jsonPath("$.nodes", hasSize(2)));
    }

    @Test
    @DisplayName("Workflow CRUD: Unknown workflow ID returns 404 WORKFLOW_NOT_FOUND")
    void testGetUnknownWorkflowReturns404() throws Exception {
        mockMvc.perform(get("/workflows/wf_nonexistent_999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("WORKFLOW_NOT_FOUND")));
    }

    @Test
    @DisplayName("Workflow Duplicate: Clones workflow as a new draft")
    void testDuplicateWorkflow() throws Exception {
        mockMvc.perform(post("/workflows/" + sampleWorkflow.getId() + "/duplicate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(is(sampleWorkflow.getId()))))
                .andExpect(jsonPath("$.name", is("Copy of " + sampleWorkflow.getName())))
                .andExpect(jsonPath("$.status", is("draft")));
    }

    @Test
    @DisplayName("Execution: Trigger synchronous workflow execution and get details")
    void testExecuteWorkflowAndGetDetails() throws Exception {
        ExecuteWorkflowRequest request = new ExecuteWorkflowRequest(Map.of("email", "lead@company.com", "score", 85));

        // 1. Trigger execution
        String responseContent = mockMvc.perform(post("/workflows/" + sampleWorkflow.getId() + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", notNullValue()))
                .andExpect(jsonPath("$.status", equalToIgnoringCase("completed")))
                .andReturn().getResponse().getContentAsString();

        ExecuteWorkflowResponse execResponse = objectMapper.readValue(responseContent, ExecuteWorkflowResponse.class);

        // 2. Fetch execution details
        mockMvc.perform(get("/executions/" + execResponse.getExecutionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(execResponse.getExecutionId())))
                .andExpect(jsonPath("$.status", equalToIgnoringCase("completed")))
                .andExpect(jsonPath("$.steps", hasSize(2)))
                .andExpect(jsonPath("$.steps[0].status", equalToIgnoringCase("completed")));

        // 3. Retry execution
        mockMvc.perform(post("/executions/" + execResponse.getExecutionId() + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", not(is(execResponse.getExecutionId()))))
                .andExpect(jsonPath("$.status", equalToIgnoringCase("completed")));
    }

    @Test
    @DisplayName("Execution: Attempting to execute a paused workflow returns 409 Conflict WORKFLOW_STATE_CONFLICT")
    void testExecutePausedWorkflowReturns409Conflict() throws Exception {
        sampleWorkflow.setStatus(WorkflowStatus.PAUSED);
        workflowRepository.save(sampleWorkflow);

        mockMvc.perform(post("/workflows/" + sampleWorkflow.getId() + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("WORKFLOW_STATE_CONFLICT")))
                .andExpect(jsonPath("$.error.message", containsString("Cannot execute a paused workflow")));
    }

    @Test
    @DisplayName("Execution: Pre-validates graph before execution and returns 400 Bad Request if graph is invalid")
    void testExecuteInvalidGraphWorkflowReturns400() throws Exception {
        WorkflowGraphDto badGraph = new WorkflowGraphDto(
                List.of(
                        new NodeDto("t1", "manual_trigger", new PositionDto(0, 0), Map.of()),
                        new NodeDto("t2", "webhook_trigger", new PositionDto(0, 50), Map.of()),
                        new NodeDto("a1", "action_email", new PositionDto(0, 100), Map.of())
                ),
                List.of(new EdgeDto("e1", "t1", "a1"), new EdgeDto("e2", "t2", "a1"))
        );
        Workflow invalidWf = workflowRepository.save(new Workflow("Broken Graph", "Two triggers", objectMapper.writeValueAsString(badGraph)));

        mockMvc.perform(post("/workflows/" + invalidWf.getId() + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("MULTIPLE_TRIGGERS")));
    }

    @Test
    @DisplayName("Templates: List templates and instantiate new workflow from template")
    void testTemplateListingAndInstantiation() throws Exception {
        mockMvc.perform(get("/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));

        List<WorkflowTemplate> templates = templateRepository.findAll();
        assertFalse(templates.isEmpty(), "Templates should be seeded");

        String templateId = templates.get(0).getId();

        mockMvc.perform(post("/templates/" + templateId + "/instantiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Custom Instantiated Workflow"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Custom Instantiated Workflow")))
                .andExpect(jsonPath("$.status", is("active")));
    }

    @Test
    @DisplayName("Webhook Ingestion: POST /api/webhooks/{workflowId} executes workflow from external event")
    void testWebhookIngestion() throws Exception {
        Map<String, Object> payload = Map.of(
                "event", "lead.created",
                "email", "webhook.lead@acme.com",
                "budget", 50000
        );

        mockMvc.perform(post("/webhooks/" + sampleWorkflow.getId())
                        .header("X-Signature", "sha256=testsignature")
                        .header("User-Agent", "AcmeWebhookClient/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", notNullValue()))
                .andExpect(jsonPath("$.workflowId", is(sampleWorkflow.getId())))
                .andExpect(jsonPath("$.status", equalToIgnoringCase("completed")));
    }

    @Test
    @DisplayName("AI Workflow Generation: POST /api/ai/generate-workflow generates connected DAG")
    void testAiGenerateWorkflowApi() throws Exception {
        AiGenerateWorkflowRequest request = new AiGenerateWorkflowRequest("When an inbound webhook arrives, send alert to Slack");

        mockMvc.perform(post("/ai/generate-workflow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflow.nodes", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.workflow.edges", hasSize(greaterThanOrEqualTo(1))));
    }
}
