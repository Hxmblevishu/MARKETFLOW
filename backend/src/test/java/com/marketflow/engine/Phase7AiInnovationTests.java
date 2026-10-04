package com.marketflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.AiGenerateWorkflowRequest;
import com.marketflow.dto.AiGenerateWorkflowResponse;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.WorkflowGraphDto;
import com.marketflow.engine.handler.AiLeadQualificationHandler;
import com.marketflow.exception.AiServiceException;
import com.marketflow.service.AiWorkflowGeneratorService;
import com.marketflow.service.GraphValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 7: AI Innovation Layer Tests (Lead Scoring & NL Generation)")
class Phase7AiInnovationTests {

    private AiLeadQualificationHandler qualificationHandler;
    private AiWorkflowGeneratorService generatorService;
    private GraphValidationService validationService;

    @BeforeEach
    void setUp() {
        JsonPathExpressionResolver resolver = new JsonPathExpressionResolver();
        qualificationHandler = new AiLeadQualificationHandler(resolver);
        validationService = new GraphValidationService();
        generatorService = new AiWorkflowGeneratorService(validationService);
    }

    @Test
    @DisplayName("AI Qualifier: Enterprise VP lead with high budget scores HOT (>=75) with hot handle")
    void testEnterpriseLeadQualification() {
        NodeDto node = new NodeDto();
        node.setId("ai_node_1");
        node.setType("ai_lead_qualifier");
        node.setData(Map.of(
                "budget", 75000,
                "companySize", 800,
                "title", "VP of Growth Marketing",
                "industry", "B2B SaaS"
        ));

        ExecutionContext context = new ExecutionContext("exec_1", "wf_1", Map.of());
        NodeExecutionResult result = qualificationHandler.execute(node, context);

        assertTrue(result.isSuccess());
        assertEquals("hot", result.getSelectedHandle());

        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        int score = (int) output.get("score");
        assertTrue(score >= 75, "Expected high enterprise score, got: " + score);
        assertEquals("HOT", output.get("tier"));
        assertEquals(true, output.get("isQualified"));
        assertNotNull(output.get("rationale"));
        assertNotNull(output.get("recommendedAction"));
    }

    @Test
    @DisplayName("AI Qualifier: Solo founder with entry budget scores COLD (<45) with cold handle")
    void testEntryLeadQualification() {
        NodeDto node = new NodeDto();
        node.setId("ai_node_2");
        node.setType("ai_lead_qualifier");
        node.setData(Map.of(
                "budget", 500,
                "companySize", 1,
                "title", "Consultant",
                "industry", "Retail"
        ));

        ExecutionContext context = new ExecutionContext("exec_2", "wf_2", Map.of());
        NodeExecutionResult result = qualificationHandler.execute(node, context);

        assertTrue(result.isSuccess());
        assertEquals("cold", result.getSelectedHandle());

        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        int score = (int) output.get("score");
        assertTrue(score < 45, "Expected cold score, got: " + score);
        assertEquals("COLD", output.get("tier"));
        assertEquals(false, output.get("isQualified"));
    }

    @Test
    @DisplayName("AI Qualifier: Resolves dynamic expressions from trigger context")
    void testDynamicExpressionInQualification() {
        NodeDto node = new NodeDto();
        node.setId("ai_node_3");
        node.setType("ai_lead_qualifier");
        node.setData(Map.of(
                "budget", "{{trigger.plannedBudget}}",
                "companySize", "{{trigger.headcount}}",
                "title", "{{trigger.jobTitle}}"
        ));

        ExecutionContext context = new ExecutionContext("exec_3", "wf_3", Map.of(
                "plannedBudget", 25000,
                "headcount", 250,
                "jobTitle", "Director of Digital Strategy"
        ));

        NodeExecutionResult result = qualificationHandler.execute(node, context);
        assertTrue(result.isSuccess());
        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        int score = (int) output.get("score");
        assertTrue(score >= 50, "Expected score >= 50, got: " + score);
        assertEquals(score, context.getVariable("lead_score"));
    }

    @Test
    @DisplayName("AI Generator: Synthesizes valid conditional workflow from natural language prompt")
    void testGenerateConditionalWorkflow() {
        String prompt = "When a lead submits an Instagram form, score them with AI. If score >= 70 notify Slack else send nurture email";
        AiGenerateWorkflowRequest request = new AiGenerateWorkflowRequest(prompt);

        AiGenerateWorkflowResponse response = generatorService.generateWorkflow(request);
        assertNotNull(response);
        assertNotNull(response.getWorkflow());

        AiGenerateWorkflowResponse.GeneratedWorkflow wf = response.getWorkflow();
        assertNotNull(wf.getName());
        assertFalse(wf.getNodes().isEmpty());
        assertFalse(wf.getEdges().isEmpty());

        // Validate that the AI-generated workflow is a completely valid DAG!
        WorkflowGraphDto graph = new WorkflowGraphDto(wf.getNodes(), wf.getEdges());
        assertDoesNotThrow(() -> validationService.validateWorkflowGraph(graph),
                "AI generated workflow must pass graph validation");

        // Verify key nodes were placed
        boolean hasAi = wf.getNodes().stream().anyMatch(n -> n.getType().equals("ai_lead_qualifier"));
        boolean hasCondition = wf.getNodes().stream().anyMatch(n -> n.getType().equals("condition"));
        boolean hasSlack = wf.getNodes().stream().anyMatch(n -> n.getType().equals("action_slack"));
        boolean hasEmail = wf.getNodes().stream().anyMatch(n -> n.getType().equals("action_email"));

        assertTrue(hasAi, "Generated workflow should contain AI scoring node");
        assertTrue(hasCondition, "Generated workflow should contain condition node");
        assertTrue(hasSlack, "Generated workflow should contain Slack action");
        assertTrue(hasEmail, "Generated workflow should contain Email action");
    }

    @Test
    @DisplayName("AI Generator: Synthesizes valid scheduled workflow from prompt")
    void testGenerateScheduledWorkflow() {
        String prompt = "Daily schedule at 9am to format contacts and sync CRM";
        AiGenerateWorkflowRequest request = new AiGenerateWorkflowRequest(prompt);

        AiGenerateWorkflowResponse response = generatorService.generateWorkflow(request);
        AiGenerateWorkflowResponse.GeneratedWorkflow wf = response.getWorkflow();

        WorkflowGraphDto graph = new WorkflowGraphDto(wf.getNodes(), wf.getEdges());
        assertDoesNotThrow(() -> validationService.validateWorkflowGraph(graph));

        NodeDto trigger = wf.getNodes().get(0);
        assertEquals("scheduled_trigger", trigger.getType());
    }

    @Test
    @DisplayName("AI Generator: Rejects blank prompt with AiServiceException")
    void testBlankPromptThrowsException() {
        assertThrows(AiServiceException.class, () -> generatorService.generateWorkflow(new AiGenerateWorkflowRequest("")));
        assertThrows(AiServiceException.class, () -> generatorService.generateWorkflow(new AiGenerateWorkflowRequest(null)));
    }

    @Test
    @DisplayName("AI Loophole: Emojis, punctuation-only, and weird whitespace do not crash name generator")
    void testEmojiAndSpecialCharPromptHandling() {
        // Punctuation-only prompt
        AiGenerateWorkflowResponse resp1 = generatorService.generateWorkflow(new AiGenerateWorkflowRequest("??? !!! @@@"));
        assertNotNull(resp1.getWorkflow().getName());
        assertFalse(resp1.getWorkflow().getName().isBlank());

        // Emoji prompt
        AiGenerateWorkflowResponse resp2 = generatorService.generateWorkflow(new AiGenerateWorkflowRequest("🚀🔥 Automate Instagram Lead Scoring and CRM Sync"));
        assertNotNull(resp2.getWorkflow().getName());
        assertTrue(resp2.getWorkflow().getName().contains("Automate"));

        // Prompt with weird tab/newline spacing
        AiGenerateWorkflowResponse resp3 = generatorService.generateWorkflow(new AiGenerateWorkflowRequest("   \t  qualify \n\n leads and   email   \t"));
        assertNotNull(resp3.getWorkflow().getName());
    }

    @Test
    @DisplayName("AI Loophole: Budget suffixes like '50k', '$100K', '1.5M' parsed accurately without degradation")
    void testBudgetSuffixParsing() {
        NodeDto node = new NodeDto();
        node.setId("ai_node_suffix");
        node.setType("ai_lead_qualifier");
        node.setData(Map.of(
                "budget", "$50k",
                "companySize", "100",
                "title", "Director",
                "industry", "SaaS"
        ));

        ExecutionContext context = new ExecutionContext("exec_suffix", "wf_suffix", Map.of());
        NodeExecutionResult result = qualificationHandler.execute(node, context);
        assertTrue(result.isSuccess());

        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        int score = (int) output.get("score");
        // Budget >= 50k gives 40 pts, size 100 gives 25 pts, director gives 20 pts, saas gives 10 pts = 95 pts (HOT)
        assertTrue(score >= 75, "Expected >= 75 for 50k budget, got: " + score);
        assertEquals("HOT", output.get("tier"));
    }

    @Test
    @DisplayName("AI Loophole: Industry inferred from company or message when industry field is omitted")
    void testIndustryInferenceFallback() {
        NodeDto node = new NodeDto();
        node.setId("ai_node_infer");
        node.setType("ai_lead_qualifier");
        node.setData(Map.of(
                "budget", 20000,
                "companySize", 50,
                "title", "VP of Sales",
                "company", "Acme Fintech Global",
                "message", "Looking for B2B SaaS automation"
        ));

        ExecutionContext context = new ExecutionContext("exec_infer", "wf_infer", Map.of());
        NodeExecutionResult result = qualificationHandler.execute(node, context);
        assertTrue(result.isSuccess());

        Map<String, Object> output = (Map<String, Object>) result.getOutputData();
        String rationale = (String) output.get("rationale");
        assertTrue(rationale.toLowerCase().contains("fintech") || rationale.toLowerCase().contains("saas"),
                "Expected rationale to mention inferred fintech/saas, got: " + rationale);
    }
}
