package com.marketflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.NodeDto;
import com.marketflow.engine.handler.ActionHandler;
import com.marketflow.engine.handler.TriggerHandler;
import com.marketflow.model.Execution;
import com.marketflow.model.ExecutionStep;
import com.marketflow.model.Workflow;
import com.marketflow.model.enums.ExecutionStatus;
import com.marketflow.model.enums.StepStatus;
import com.marketflow.repository.ExecutionRepository;
import com.marketflow.repository.ExecutionStepRepository;
import com.marketflow.repository.WorkflowRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 6:
 * Integration Node Handlers (Triggers & External Actions) and End-to-End Dynamic DAG Execution.
 */
@SpringBootTest
class Phase6IntegrationHandlersTests {

    @Autowired
    private TriggerHandler triggerHandler;

    @Autowired
    private ActionHandler actionHandler;

    @Autowired
    private DagExecutionEngine dagExecutionEngine;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionStepRepository stepRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        Map<String, Object> leadInput = Map.of(
                "email", "priya.sharma@growthhub.com",
                "name", "Priya Sharma",
                "company", "GrowthHub Media",
                "score", 92,
                "tier", "ENTERPRISE",
                "budget", 25000
        );
        context = new ExecutionContext("exec_phase6_test", "wf_phase6_test", leadInput);
    }

    @AfterEach
    void tearDown() {
        stepRepository.deleteAll();
        executionRepository.deleteAll();
        workflowRepository.deleteAll();
    }

    @Test
    @DisplayName("TriggerHandler: should execute manual, webhook, and scheduled triggers")
    void testTriggerSubtypes() {
        // 1. Manual trigger
        NodeDto manualNode = new NodeDto();
        manualNode.setId("trig_manual");
        manualNode.setType("manual_trigger");
        manualNode.setLabel("Manual Lead Entry");

        NodeExecutionResult manualResult = triggerHandler.execute(manualNode, context);
        assertTrue(manualResult.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> manualOutput = (Map<String, Object>) manualResult.getOutputData();
        assertEquals("manual_trigger", manualOutput.get("triggerType"));
        assertEquals("MANUAL_UI", manualOutput.get("source"));
        assertEquals("priya.sharma@growthhub.com", manualOutput.get("email"));

        // 2. Webhook trigger
        NodeDto webhookNode = new NodeDto();
        webhookNode.setId("trig_webhook");
        webhookNode.setType("webhook_trigger");
        webhookNode.setData(Map.of("webhookId", "wh_instagram_leads_123"));

        NodeExecutionResult webhookResult = triggerHandler.execute(webhookNode, context);
        assertTrue(webhookResult.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> webhookOutput = (Map<String, Object>) webhookResult.getOutputData();
        assertEquals("webhook_trigger", webhookOutput.get("triggerType"));
        assertEquals("wh_instagram_leads_123", webhookOutput.get("webhookId"));
        assertEquals("HTTP_POST", webhookOutput.get("source"));

        // 3. Scheduled trigger
        NodeDto scheduledNode = new NodeDto();
        scheduledNode.setId("trig_scheduled");
        scheduledNode.setType("scheduled_trigger");
        scheduledNode.setData(Map.of("cron", "0 9 * * 1-5", "timezone", "America/New_York"));

        NodeExecutionResult scheduledResult = triggerHandler.execute(scheduledNode, context);
        assertTrue(scheduledResult.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> schedOutput = (Map<String, Object>) scheduledResult.getOutputData();
        assertEquals("scheduled_trigger", schedOutput.get("triggerType"));
        assertEquals("0 9 * * 1-5", schedOutput.get("cron"));
        assertEquals("America/New_York", schedOutput.get("timezone"));
    }

    @Test
    @DisplayName("ActionHandler: Email action should resolve dynamic template fields and produce delivery receipt")
    void testEmailAction() {
        NodeDto emailNode = new NodeDto();
        emailNode.setId("act_email");
        emailNode.setType("action_email");
        emailNode.setData(Map.of(
                "to", "{{trigger.email}}",
                "subject", "Welcome {{trigger.name}} to Marketflow!",
                "body", "Hello {{trigger.name}}, thank you for registering with {{trigger.company}}.",
                "from", "sales@marketflow.io"
        ));

        NodeExecutionResult result = actionHandler.execute(emailNode, context);
        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        assertEquals("action_email", output.get("actionType"));
        assertEquals("SUCCESS", output.get("status"));
        assertEquals("priya.sharma@growthhub.com", output.get("recipient"));
        assertEquals("Welcome Priya Sharma to Marketflow!", output.get("subject"));
        assertEquals("Hello Priya Sharma, thank you for registering with GrowthHub Media.", output.get("body"));
        assertNotNull(output.get("receiptId"));
        assertTrue(output.get("receiptId").toString().startsWith("email_rcpt_"));
    }

    @Test
    @DisplayName("ActionHandler: Slack action should resolve channel and message templates")
    void testSlackAction() {
        NodeDto slackNode = new NodeDto();
        slackNode.setId("act_slack");
        slackNode.setType("action_slack");
        slackNode.setData(Map.of(
                "channel", "#growth-leads",
                "message", "🚀 High-Value Lead Alert: {{trigger.name}} (Budget: ${{trigger.budget}})"
        ));

        NodeExecutionResult result = actionHandler.execute(slackNode, context);
        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        assertEquals("action_slack", output.get("actionType"));
        assertEquals("#growth-leads", output.get("channel"));
        assertEquals("🚀 High-Value Lead Alert: Priya Sharma (Budget: $25000)", output.get("message"));
        assertNotNull(output.get("receiptId"));
        assertTrue(output.get("receiptId").toString().startsWith("slack_msg_"));
    }

    @Test
    @DisplayName("ActionHandler: CRM action should simulate HubSpot/Salesforce record creation with record IDs")
    void testCrmAction() {
        NodeDto crmNode = new NodeDto();
        crmNode.setId("act_crm");
        crmNode.setType("action_crm");
        crmNode.setData(Map.of(
                "name", "{{trigger.name}}",
                "email", "{{trigger.email}}",
                "company", "{{trigger.company}}",
                "score", "{{trigger.score}}",
                "stage", "SQL",
                "crmSystem", "HubSpot CRM"
        ));

        NodeExecutionResult result = actionHandler.execute(crmNode, context);
        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        assertEquals("action_crm", output.get("actionType"));
        assertEquals("HubSpot CRM", output.get("crmSystem"));
        assertNotNull(output.get("recordId"));
        assertTrue(output.get("recordId").toString().startsWith("crm_lead_"));

        @SuppressWarnings("unchecked")
        Map<String, Object> leadData = (Map<String, Object>) output.get("lead");
        assertEquals("Priya Sharma", leadData.get("name"));
        assertEquals("priya.sharma@growthhub.com", leadData.get("email"));
        assertEquals("GrowthHub Media", leadData.get("company"));
        assertEquals("SQL", leadData.get("stage"));
    }

    @Test
    @DisplayName("ActionHandler: HTTP action should handle outbound requests and safe offline simulation")
    void testHttpAction() {
        NodeDto httpNode = new NodeDto();
        httpNode.setId("act_http");
        httpNode.setType("action_http");
        httpNode.setData(Map.of(
                "url", "https://api.mockcrm.io/v1/leads/{{trigger.email}}",
                "method", "POST",
                "headers", Map.of("Authorization", "Bearer mock-token-123"),
                "body", Map.of("leadName", "{{trigger.name}}", "tier", "{{trigger.tier}}"),
                "failOnError", false
        ));

        NodeExecutionResult result = actionHandler.execute(httpNode, context);
        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        assertEquals("action_http", output.get("actionType"));
        assertEquals("https://api.mockcrm.io/v1/leads/priya.sharma@growthhub.com", output.get("requestUrl"));
        assertEquals("POST", output.get("method"));
        assertNotNull(output.get("status"));
    }

    @Test
    @DisplayName("End-to-End DAG: Trigger -> Transform -> Condition -> Slack with dynamic expression passing")
    void testEndToEndPhase5And6Workflow() {
        String workflowJson = """
        {
          "nodes": [
            {
              "id": "node_trigger",
              "type": "trigger_webhook",
              "position": {"x": 100, "y": 100},
              "data": {"label": "Inbound Webhook"}
            },
            {
              "id": "node_transform",
              "type": "transform",
              "position": {"x": 100, "y": 200},
              "data": {
                "mappings": {
                  "fullName": "{{trigger.name}}",
                  "companyUpper": "{{trigger.company}}"
                },
                "fields": [
                  {"source": "{{trigger.score}}", "target": "computedScore", "type": "number"}
                ]
              }
            },
            {
              "id": "node_condition",
              "type": "condition",
              "position": {"x": 100, "y": 300},
              "data": {
                "field": "{{node_transform.computedScore}}",
                "operator": ">=",
                "value": 80
              }
            },
            {
              "id": "node_slack",
              "type": "action_slack",
              "position": {"x": 50, "y": 400},
              "data": {
                "channel": "#vip-leads",
                "message": "Qualified VIP Lead: {{node_transform.fullName}} from {{node_transform.companyUpper}} (Score: {{node_transform.computedScore}})"
              }
            },
            {
              "id": "node_email",
              "type": "action_email",
              "position": {"x": 250, "y": 400},
              "data": {
                "subject": "Standard Welcome",
                "to": "{{trigger.email}}"
              }
            }
          ],
          "edges": [
            {"id": "e1", "source": "node_trigger", "target": "node_transform"},
            {"id": "e2", "source": "node_transform", "target": "node_condition"},
            {"id": "e3", "source": "node_condition", "target": "node_slack", "sourceHandle": "true"},
            {"id": "e4", "source": "node_condition", "target": "node_email", "sourceHandle": "false"}
          ]
        }
        """;

        Workflow wf = workflowRepository.save(new Workflow("E2E Phase 5 & 6 Pipeline", "End-to-end integration test", workflowJson));
        Execution exec = executionRepository.save(new Execution(wf, "{}"));

        Map<String, Object> input = Map.of(
                "name", "Priya Sharma",
                "company", "GrowthHub Media",
                "email", "priya@growthhub.com",
                "score", 95
        );

        Execution result = dagExecutionEngine.execute(wf, exec, input);

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertNull(result.getErrorMessage());

        List<ExecutionStep> steps = result.getSteps();
        assertEquals(4, steps.size(), "Should execute: Trigger -> Transform -> Condition -> Slack");

        assertEquals("node_trigger", steps.get(0).getNodeId());
        assertEquals("node_transform", steps.get(1).getNodeId());
        assertEquals("node_condition", steps.get(2).getNodeId());
        assertEquals("node_slack", steps.get(3).getNodeId());

        for (ExecutionStep step : steps) {
            assertEquals(StepStatus.COMPLETED, step.getStatus());
        }

        // Verify that Slack output contains the dynamically transformed data from upstream node
        String slackOutput = steps.get(3).getOutputData();
        assertTrue(slackOutput.contains("#vip-leads"), "Should dispatch to configured channel");
        assertTrue(slackOutput.contains("Priya Sharma"), "Should interpolate fullName from transform node");
        assertTrue(slackOutput.contains("GrowthHub Media"), "Should interpolate companyUpper from transform node");
        assertTrue(slackOutput.contains("95"), "Should interpolate computedScore");
    }
}
