package com.marketflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.EdgeDto;
import com.marketflow.dto.NodeDto;
import com.marketflow.dto.PositionDto;
import com.marketflow.dto.WorkflowGraphDto;
import com.marketflow.model.WorkflowTemplate;
import com.marketflow.repository.WorkflowTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Automatically seeds production-grade marketing templates when the database is empty.
 */
@Component
public class WorkflowTemplateDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateDataSeeder.class);

    private final WorkflowTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public WorkflowTemplateDataSeeder(WorkflowTemplateRepository templateRepository, ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        if (templateRepository.count() > 0) {
            log.info("Workflow templates already seeded (count: {}). Skipping seeder.", templateRepository.count());
            return;
        }

        log.info("Seeding initial workflow templates into database...");

        try {
            // Template 1: AI Lead Qualification & Routing
            WorkflowGraphDto t1Graph = new WorkflowGraphDto(
                    List.of(
                            createNode("t1_trigger", "webhook_trigger", 250, 50, Map.of("label", "Inbound Lead Webhook")),
                            createNode("t1_ai", "ai_lead_qualifier", 250, 180, Map.of("label", "AI Lead Scoring", "threshold", 60)),
                            createNode("t1_cond", "condition", 250, 310, Map.of("label", "Is Hot Lead?", "field", "score", "operator", ">=", "value", 60)),
                            createNode("t1_slack", "action_slack", 100, 440, Map.of("label", "Alert Sales on Slack", "channel", "#sales-hot-leads")),
                            createNode("t1_email", "action_email", 400, 440, Map.of("label", "Send Drip Nurture", "subject", "Welcome to Marketflow"))
                    ),
                    List.of(
                            new EdgeDto("e1", "t1_trigger", "t1_ai"),
                            new EdgeDto("e2", "t1_ai", "t1_cond"),
                            createEdge("e3", "t1_cond", "t1_slack", "true", "Qualified (>= 60)"),
                            createEdge("e4", "t1_cond", "t1_email", "false", "Nurture (< 60)")
                    )
            );

            // Template 2: Event Registration & CRM Sync
            WorkflowGraphDto t2Graph = new WorkflowGraphDto(
                    List.of(
                            createNode("t2_trigger", "manual_trigger", 250, 50, Map.of("label", "Event Registration Form")),
                            createNode("t2_transform", "transform", 250, 180, Map.of("label", "Normalize Contact Data",
                                    "mapping", Map.of("leadEmail", "{{trigger.email}}", "normalizedCompany", "{{trigger.company}}"))),
                            createNode("t2_crm", "action_crm", 250, 310, Map.of("label", "HubSpot Contact Creation", "pipeline", "Event Attendees")),
                            createNode("t2_email", "action_email", 250, 440, Map.of("label", "Send Ticket Confirmation", "subject", "Your Event Pass is Ready!"))
                    ),
                    List.of(
                            new EdgeDto("e2_1", "t2_trigger", "t2_transform"),
                            new EdgeDto("e2_2", "t2_transform", "t2_crm"),
                            new EdgeDto("e2_3", "t2_crm", "t2_email")
                    )
            );

            templateRepository.save(new WorkflowTemplate(
                    "AI Lead Scoring & Omnichannel Routing",
                    "Lead Generation",
                    "Automatically evaluates inbound leads using AI scoring and directs hot prospects to Slack while nurturing others via email.",
                    objectMapper.writeValueAsString(t1Graph)
            ));

            templateRepository.save(new WorkflowTemplate(
                    "Event Registration & CRM Sync",
                    "Event Marketing",
                    "Ingests event attendees, standardizes company information, provisions CRM records, and issues confirmation passes.",
                    objectMapper.writeValueAsString(t2Graph)
            ));

            log.info("Successfully seeded 2 workflow templates.");
        } catch (Exception ex) {
            log.error("Failed to seed workflow templates: {}", ex.getMessage(), ex);
        }
    }

    private NodeDto createNode(String id, String type, int x, int y, Map<String, Object> data) {
        NodeDto node = new NodeDto();
        node.setId(id);
        node.setType(type);
        node.setPosition(new PositionDto(x, y));
        node.setData(data);
        return node;
    }

    private EdgeDto createEdge(String id, String source, String target, String handle, String label) {
        EdgeDto edge = new EdgeDto(id, source, target);
        edge.setSourceHandle(handle);
        edge.setLabel(label);
        return edge;
    }
}
