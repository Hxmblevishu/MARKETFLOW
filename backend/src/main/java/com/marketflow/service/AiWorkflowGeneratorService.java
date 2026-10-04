package com.marketflow.service;

import com.marketflow.dto.AiGenerateWorkflowRequest;
import com.marketflow.dto.AiGenerateWorkflowResponse;
import com.marketflow.dto.EdgeDto;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.PositionDto;
import com.marketflow.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Phase 7: Innovation Layer - Natural Language Workflow Generator
 *
 * Transforms high-level human prompts into fully-formed, valid DAG workflow specifications
 * compatible with React Flow and executable by the DagExecutionEngine.
 */
@Service
public class AiWorkflowGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AiWorkflowGeneratorService.class);

    private final GraphValidationService validationService;

    public AiWorkflowGeneratorService(GraphValidationService validationService) {
        this.validationService = validationService;
    }

    public AiGenerateWorkflowResponse generateWorkflow(AiGenerateWorkflowRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiServiceException("AI_GENERATOR", "Prompt cannot be empty for workflow generation");
        }

        String prompt = request.getPrompt().trim();
        log.info("Generating workflow from prompt: '{}'", prompt);

        try {
            AiGenerateWorkflowResponse.GeneratedWorkflow workflow = synthesizeWorkflowFromPrompt(prompt);
            return new AiGenerateWorkflowResponse(workflow);
        } catch (Exception ex) {
            log.error("Failed to generate workflow from prompt: {}", ex.getMessage(), ex);
            throw new AiServiceException("AI_GENERATOR", "Workflow generation failed: " + ex.getMessage(), ex);
        }
    }

    private AiGenerateWorkflowResponse.GeneratedWorkflow synthesizeWorkflowFromPrompt(String prompt) {
        String lower = prompt.toLowerCase();

        List<NodeDto> nodes = new ArrayList<>();
        List<EdgeDto> edges = new ArrayList<>();

        int y = 50;
        int x = 250;

        // 1. Determine Trigger Node
        NodeDto triggerNode = new NodeDto();
        triggerNode.setId("node_trigger");
        triggerNode.setPosition(new PositionDto(x, y));

        Map<String, Object> triggerData = new LinkedHashMap<>();
        if (lower.contains("webhook") || lower.contains("api") || lower.contains("event")) {
            triggerNode.setType("webhook_trigger");
            triggerData.put("label", "Webhook Trigger");
            triggerData.put("webhookId", "hook_" + UUID.randomUUID().toString().substring(0, 8));
        } else if (lower.contains("schedule") || lower.contains("daily") || lower.contains("hourly") || lower.contains("cron")) {
            triggerNode.setType("scheduled_trigger");
            triggerData.put("label", "Scheduled Trigger");
            triggerData.put("cron", "0 0 9 * * ?");
        } else {
            triggerNode.setType("manual_trigger");
            triggerData.put("label", "Lead Form Ingestion");
            triggerData.put("source", "marketing_landing_page");
        }
        triggerNode.setData(triggerData);
        nodes.add(triggerNode);

        String previousNodeId = triggerNode.getId();
        y += 130;

        // 2. AI Lead Qualification Node if requested or implied
        boolean hasAiScoring = lower.contains("qualify") || lower.contains("score") || lower.contains("ai") || lower.contains("lead");
        if (hasAiScoring) {
            NodeDto aiNode = new NodeDto();
            aiNode.setId("node_ai_scoring");
            aiNode.setType("ai_lead_qualifier");
            aiNode.setPosition(new PositionDto(x, y));

            Map<String, Object> aiData = new LinkedHashMap<>();
            aiData.put("label", "AI Lead Scoring");
            aiData.put("model", "gpt-4o-mini");
            aiData.put("threshold", 70);
            aiNode.setData(aiData);
            nodes.add(aiNode);

            edges.add(new EdgeDto("edge_" + previousNodeId + "_" + aiNode.getId(), previousNodeId, aiNode.getId()));
            previousNodeId = aiNode.getId();
            y += 130;
        }

        // 3. Transformation / Data Mapping if requested
        if (lower.contains("transform") || lower.contains("map") || lower.contains("clean") || lower.contains("format")) {
            NodeDto transformNode = new NodeDto();
            transformNode.setId("node_transform");
            transformNode.setType("transform");
            transformNode.setPosition(new PositionDto(x, y));

            Map<String, Object> transData = new LinkedHashMap<>();
            transData.put("label", "Format Contact Info");
            transData.put("mapping", Map.of(
                    "normalizedEmail", "{{trigger.email}}",
                    "fullName", "{{trigger.first_name}} {{trigger.last_name}}"
            ));
            transformNode.setData(transData);
            nodes.add(transformNode);

            edges.add(new EdgeDto("edge_" + previousNodeId + "_" + transformNode.getId(), previousNodeId, transformNode.getId()));
            previousNodeId = transformNode.getId();
            y += 130;
        }

        // 4. Conditional Branching
        boolean hasCondition = lower.contains("if") || lower.contains("score >") || lower.contains("condition") || lower.contains("hot");
        if (hasCondition) {
            NodeDto conditionNode = new NodeDto();
            conditionNode.setId("node_condition");
            conditionNode.setType("condition");
            conditionNode.setPosition(new PositionDto(x, y));

            Map<String, Object> condData = new LinkedHashMap<>();
            condData.put("label", "Check Qualification");
            condData.put("field", "score");
            condData.put("operator", ">=");
            condData.put("value", 70);
            conditionNode.setData(condData);
            nodes.add(conditionNode);

            edges.add(new EdgeDto("edge_" + previousNodeId + "_" + conditionNode.getId(), previousNodeId, conditionNode.getId()));
            y += 130;

            // True Branch Action (e.g., Slack or CRM)
            NodeDto trueAction = new NodeDto();
            trueAction.setId("node_action_qualified");
            trueAction.setType(lower.contains("crm") ? "action_crm" : "action_slack");
            trueAction.setPosition(new PositionDto(x - 140, y));

            Map<String, Object> trueData = new LinkedHashMap<>();
            if (trueAction.getType().equals("action_slack")) {
                trueData.put("label", "Notify Sales on Slack");
                trueData.put("channel", "#enterprise-leads");
                trueData.put("message", "High value lead qualified! Score: {{step_1.output.score}}");
            } else {
                trueData.put("label", "Create CRM Opportunity");
                trueData.put("pipeline", "Sales Qualified Leads");
            }
            trueAction.setData(trueData);
            nodes.add(trueAction);

            EdgeDto trueEdge = new EdgeDto("edge_cond_true", conditionNode.getId(), trueAction.getId());
            trueEdge.setSourceHandle("true");
            trueEdge.setLabel("Qualified (>= 70)");
            edges.add(trueEdge);

            // False Branch Action (e.g., Nurture Email)
            NodeDto falseAction = new NodeDto();
            falseAction.setId("node_action_unqualified");
            falseAction.setType("action_email");
            falseAction.setPosition(new PositionDto(x + 140, y));

            Map<String, Object> falseData = new LinkedHashMap<>();
            falseData.put("label", "Send Nurture Drip");
            falseData.put("subject", "Resources to accelerate your growth");
            falseData.put("template", "lead_nurture_intro");
            falseAction.setData(falseData);
            nodes.add(falseAction);

            EdgeDto falseEdge = new EdgeDto("edge_cond_false", conditionNode.getId(), falseAction.getId());
            falseEdge.setSourceHandle("false");
            falseEdge.setLabel("Nurture (< 70)");
            edges.add(falseEdge);

        } else {
            // Linear action path
            NodeDto actionNode = new NodeDto();
            actionNode.setId("node_action_1");
            actionNode.setPosition(new PositionDto(x, y));

            Map<String, Object> actionData = new LinkedHashMap<>();
            if (lower.contains("slack")) {
                actionNode.setType("action_slack");
                actionData.put("label", "Post Slack Alert");
                actionData.put("channel", "#marketing-events");
            } else if (lower.contains("crm")) {
                actionNode.setType("action_crm");
                actionData.put("label", "Sync to CRM");
            } else {
                actionNode.setType("action_email");
                actionData.put("label", "Send Confirmation Email");
                actionData.put("subject", "Welcome to Marketflow");
            }
            actionNode.setData(actionData);
            nodes.add(actionNode);

            edges.add(new EdgeDto("edge_" + previousNodeId + "_" + actionNode.getId(), previousNodeId, actionNode.getId()));
        }

        String generatedName = extractWorkflowName(prompt);
        String description = "AI-generated workflow based on prompt: \"" + prompt + "\"";

        return new AiGenerateWorkflowResponse.GeneratedWorkflow(generatedName, description, nodes, edges);
    }

    private String extractWorkflowName(String prompt) {
        String clean = prompt.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (clean.length() <= 35) {
            return clean.substring(0, 1).toUpperCase() + clean.substring(1);
        }
        String[] words = clean.split("\\s+");
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < Math.min(5, words.length); i++) {
            name.append(words[i].substring(0, 1).toUpperCase()).append(words[i].substring(1)).append(" ");
        }
        return name.toString().trim() + " Flow";
    }
}
