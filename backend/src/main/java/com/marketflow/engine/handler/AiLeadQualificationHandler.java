package com.marketflow.engine.handler;

import com.marketflow.dto.NodeDto;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.JsonPathExpressionResolver;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.NodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 7: Innovation Layer - AI Lead Qualification Handler
 *
 * Evaluates inbound leads on budget, company size, executive decision authority,
 * and business domain fit. Generates qualification scores (0-100), tiers (HOT, WARM, COLD),
 * and strategic rationale for downstream routing.
 */
@Component
public class AiLeadQualificationHandler implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiLeadQualificationHandler.class);
    private final JsonPathExpressionResolver expressionResolver;

    public AiLeadQualificationHandler(JsonPathExpressionResolver expressionResolver) {
        this.expressionResolver = expressionResolver;
    }

    @Override
    public boolean supports(String nodeType) {
        if (nodeType == null) return false;
        String type = nodeType.toLowerCase().trim();
        return type.equals("ai_lead_qualifier")
                || type.equals("ai_qualify_lead")
                || type.equals("ai_lead_score")
                || type.equals("lead_qualifier");
    }

    @Override
    public NodeExecutionResult execute(NodeDto node, ExecutionContext context) {
        log.info("Executing AI Lead Qualification for node [{}] in execution [{}]", node.getId(), context.getExecutionId());

        Map<String, Object> nodeConfig = node.getData() != null ? node.getData() : Map.of();
        Map<String, Object> resolvedConfig = expressionResolver.resolveMap(nodeConfig, context);

        // Extract lead data from resolvedConfig or fallback to context variables
        Map<String, Object> leadData = extractLeadData(resolvedConfig, context);

        // Perform intelligent heuristic lead qualification
        LeadQualificationResult result = scoreLead(leadData);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("score", result.score);
        output.put("qualification", result.tier);
        output.put("isQualified", result.score >= 50);
        output.put("tier", result.tier);
        output.put("rationale", result.rationale);
        output.put("recommendedAction", result.recommendedAction);
        output.put("analyzedLead", leadData);
        output.put("evaluatedAt", Instant.now().toString());

        // Update context with lead score
        context.setVariable("lead_score", result.score);
        context.setVariable("lead_tier", result.tier);

        // Branching handle: 'hot' (>=75), 'warm' (45-74), 'cold' (<45), or 'true'/'false'
        String sourceHandle = result.score >= 50 ? "true" : "false";
        if (result.tier.equalsIgnoreCase("HOT")) {
            sourceHandle = "hot";
        } else if (result.tier.equalsIgnoreCase("WARM")) {
            sourceHandle = "warm";
        } else if (result.tier.equalsIgnoreCase("COLD")) {
            sourceHandle = "cold";
        }

        return NodeExecutionResult.conditional(node.getId(), output, sourceHandle);
    }

    private Map<String, Object> extractLeadData(Map<String, Object> config, ExecutionContext context) {
        Map<String, Object> lead = new LinkedHashMap<>();

        // Check if config has explicit fields
        putIfPresent(lead, "email", config.get("email"));
        putIfPresent(lead, "company", config.get("company"));
        putIfPresent(lead, "budget", config.get("budget"));
        putIfPresent(lead, "companySize", config.get("companySize"));
        putIfPresent(lead, "title", config.get("title"));
        putIfPresent(lead, "industry", config.get("industry"));
        putIfPresent(lead, "message", config.get("message"));

        // Fallback to trigger payload / variables
        Map<String, Object> trigger = context.getTriggerPayload();
        if (trigger != null) {
            trigger.forEach((k, v) -> lead.putIfAbsent(k, v));
        }

        return lead;
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null && !value.toString().isBlank()) {
            map.put(key, value);
        }
    }

    private LeadQualificationResult scoreLead(Map<String, Object> lead) {
        int score = 0;
        StringBuilder rationale = new StringBuilder();

        // 1. Budget Assessment (up to 40 pts)
        double budget = parseNumeric(lead.get("budget"));
        if (budget >= 50000) {
            score += 40;
            rationale.append("High enterprise budget (+$40k+). ");
        } else if (budget >= 15000) {
            score += 30;
            rationale.append("Mid-market budget ($15k-$50k). ");
        } else if (budget >= 5000) {
            score += 20;
            rationale.append("Growth budget ($5k-$15k). ");
        } else if (budget > 0) {
            score += 10;
            rationale.append("Entry budget (<$5k). ");
        }

        // 2. Company Size Assessment (up to 30 pts)
        double size = parseNumeric(lead.get("companySize"));
        if (size >= 500) {
            score += 30;
            rationale.append("Enterprise scale (>500 employees). ");
        } else if (size >= 100) {
            score += 25;
            rationale.append("Mid-market scale (100-500 employees). ");
        } else if (size >= 20) {
            score += 15;
            rationale.append("SMB scale (20-100 employees). ");
        } else if (size > 0) {
            score += 5;
            rationale.append("Startup/Solo (<20 employees). ");
        }

        // 3. Job Title Authority (up to 20 pts)
        String title = String.valueOf(lead.getOrDefault("title", "")).toLowerCase();
        if (title.contains("vp") || title.contains("vice president") || title.contains("director")
                || title.contains("c-level") || title.contains("cto") || title.contains("ceo")
                || title.contains("cmo") || title.contains("founder") || title.contains("head")) {
            score += 20;
            rationale.append("Executive decision-maker title. ");
        } else if (title.contains("lead") || title.contains("manager") || title.contains("architect")) {
            score += 12;
            rationale.append("Management influencer title. ");
        } else if (!title.isBlank()) {
            score += 5;
            rationale.append("Individual contributor title. ");
        }

        // 4. Industry Fit (up to 10 pts)
        String industry = String.valueOf(lead.getOrDefault("industry", "")).toLowerCase().trim();
        if (industry.isBlank()) {
            String company = String.valueOf(lead.getOrDefault("company", "")).toLowerCase();
            String message = String.valueOf(lead.getOrDefault("message", "")).toLowerCase();
            if (company.contains("fintech") || message.contains("fintech")) industry = "fintech";
            else if (company.contains("saas") || message.contains("saas")) industry = "saas";
            else if (company.contains("ecommerce") || message.contains("ecommerce") || message.contains("e-commerce")) industry = "e-commerce";
            else if (company.contains("tech") || message.contains("tech")) industry = "tech";
            else if (company.contains("health") || message.contains("health")) industry = "healthcare";
        }

        if (industry.contains("saas") || industry.contains("tech") || industry.contains("software")
                || industry.contains("e-commerce") || industry.contains("fintech") || industry.contains("healthcare")) {
            score += 10;
            rationale.append("High-fit target vertical (").append(industry).append(").");
        } else if (!industry.isBlank()) {
            score += 5;
            rationale.append("Standard industry vertical.");
        }

        // Normalize 0 - 100
        int finalScore = Math.min(100, Math.max(0, score));
        String tier;
        String recommendation;

        if (finalScore >= 75) {
            tier = "HOT";
            recommendation = "Immediate direct sales outreach and high-priority Slack notification.";
        } else if (finalScore >= 45) {
            tier = "WARM";
            recommendation = "Route to automated email nurture campaign and SDR follow-up within 24h.";
        } else {
            tier = "COLD";
            recommendation = "Add to monthly newsletter drip sequence.";
        }

        return new LeadQualificationResult(finalScore, tier, rationale.toString().trim(), recommendation);
    }

    private double parseNumeric(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        try {
            String str = val.toString().trim().toLowerCase();
            double multiplier = 1.0;
            if (str.endsWith("k")) {
                multiplier = 1000.0;
                str = str.substring(0, str.length() - 1);
            } else if (str.endsWith("m")) {
                multiplier = 1000000.0;
                str = str.substring(0, str.length() - 1);
            }
            String clean = str.replaceAll("[^0-9.]", "");
            if (clean.isBlank()) return 0;
            return Double.parseDouble(clean) * multiplier;
        } catch (Exception e) {
            return 0;
        }
    }

    private static class LeadQualificationResult {
        final int score;
        final String tier;
        final String rationale;
        final String recommendedAction;

        LeadQualificationResult(int score, String tier, String rationale, String recommendedAction) {
            this.score = score;
            this.tier = tier;
            this.rationale = rationale;
            this.recommendedAction = recommendedAction;
        }
    }
}
