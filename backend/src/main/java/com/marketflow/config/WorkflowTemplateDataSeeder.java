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
    private final com.marketflow.repository.WorkflowRepository workflowRepository;
    private final com.marketflow.repository.ExecutionRepository executionRepository;
    private final com.marketflow.repository.ExecutionStepRepository stepRepository;
    private final com.marketflow.repository.UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public WorkflowTemplateDataSeeder(WorkflowTemplateRepository templateRepository,
                                      com.marketflow.repository.WorkflowRepository workflowRepository,
                                      com.marketflow.repository.ExecutionRepository executionRepository,
                                      com.marketflow.repository.ExecutionStepRepository stepRepository,
                                      com.marketflow.repository.UserRepository userRepository,
                                      ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        seedDemoUser();
        seedTemplates();
        seedDemoWorkflowAndExecutions();
    }

    private void seedDemoUser() {
        if (!userRepository.existsByEmail("judge@marketflow.demo")) {
            log.info("Seeding default demo judge user into database...");
            org.springframework.security.crypto.password.PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            com.marketflow.model.User judge = new com.marketflow.model.User(
                    "judge@marketflow.demo",
                    encoder.encode("JudgeDemo2026!"),
                    "Marketflow Demo Judge",
                    com.marketflow.model.enums.UserRole.ROLE_USER
            );
            userRepository.save(judge);
            log.info("Default judge user seeded: judge@marketflow.demo");
        }
    }

    private void seedTemplates() {
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

            // Template 3: Abandoned Cart Recovery & VIP Escalation
            WorkflowGraphDto t3Graph = new WorkflowGraphDto(
                    List.of(
                            createNode("t3_trigger", "webhook_trigger", 250, 50, Map.of("label", "Shopify Abandoned Checkout")),
                            createNode("t3_cond", "condition", 250, 180, Map.of("label", "High Value Cart (>$200)?", "field", "cartValue", "operator", ">=", "value", 200)),
                            createNode("t3_slack", "action_slack", 100, 310, Map.of("label", "Notify VIP Concierge", "channel", "#vip-sales")),
                            createNode("t3_email", "action_email", 400, 310, Map.of("label", "Send 10% Discount Drip", "subject", "Complete your order with 10% off!"))
                    ),
                    List.of(
                            new EdgeDto("e3_1", "t3_trigger", "t3_cond"),
                            createEdge("e3_2", "t3_cond", "t3_slack", "true", "VIP Cart (>= $200)"),
                            createEdge("e3_3", "t3_cond", "t3_email", "false", "Standard Cart (< $200)")
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

            templateRepository.save(new WorkflowTemplate(
                    "Abandoned Cart Recovery & VIP Escalation",
                    "E-Commerce",
                    "Monitors high-value checkout abandonments, alerts sales for concierge recovery, and issues recovery discount codes.",
                    objectMapper.writeValueAsString(t3Graph)
            ));

            log.info("Successfully seeded 3 workflow templates.");
        } catch (Exception ex) {
            log.error("Failed to seed workflow templates: {}", ex.getMessage(), ex);
        }
    }

    private void seedDemoWorkflowAndExecutions() {
        if (workflowRepository.count() > 0) {
            return;
        }

        try {
            WorkflowGraphDto demoGraph = new WorkflowGraphDto(
                    List.of(
                            createNode("demo_trig", "webhook_trigger", 250, 50, Map.of("label", "Instagram Lead Webhook")),
                            createNode("demo_ai", "ai_lead_qualifier", 250, 180, Map.of("label", "AI Lead Scoring", "threshold", 70)),
                            createNode("demo_cond", "condition", 250, 310, Map.of("label", "Score >= 70?", "field", "score", "operator", ">=", "value", 70)),
                            createNode("demo_slack", "action_slack", 100, 440, Map.of("label", "Sales Alert Slack", "channel", "#enterprise-leads")),
                            createNode("demo_email", "action_email", 400, 440, Map.of("label", "Nurture Email", "subject", "Accelerate with Marketflow"))
                    ),
                    List.of(
                            new EdgeDto("de1", "demo_trig", "demo_ai"),
                            new EdgeDto("de2", "demo_ai", "demo_cond"),
                            createEdge("de3", "demo_cond", "demo_slack", "true", "Qualified (>= 70)"),
                            createEdge("de4", "demo_cond", "demo_email", "false", "Nurture (< 70)")
                    )
            );

            com.marketflow.model.Workflow demoWorkflow = new com.marketflow.model.Workflow(
                    "Instagram Lead Qualification Pipeline",
                    "Live production pipeline demonstrating AI lead scoring, conditional branching, and multi-channel alerting.",
                    objectMapper.writeValueAsString(demoGraph)
            );
            demoWorkflow.setId("wf_001");
            demoWorkflow.setStatus(com.marketflow.model.enums.WorkflowStatus.ACTIVE);
            com.marketflow.model.Workflow savedWf = workflowRepository.save(demoWorkflow);

            // Seed 1 Successful High-Value Execution
            com.marketflow.model.Execution exec1 = new com.marketflow.model.Execution(savedWf, "{\"email\":\"aarav.sharma@enterprise.com\",\"company\":\"TechCorp\",\"budget\":75000,\"title\":\"VP Growth\"}");
            exec1.setStatus(com.marketflow.model.enums.ExecutionStatus.COMPLETED);
            exec1.setStartedAt(java.time.Instant.now().minusSeconds(3600));
            exec1.setCompletedAt(java.time.Instant.now().minusSeconds(3598));
            exec1.setOutputData("{\"score\":88,\"tier\":\"HOT\",\"qualified\":true}");
            com.marketflow.model.Execution savedExec1 = executionRepository.save(exec1);

            com.marketflow.model.ExecutionStep step1 = new com.marketflow.model.ExecutionStep(savedExec1, "demo_trig", "webhook_trigger", "Instagram Lead Webhook");
            step1.setStatus(com.marketflow.model.enums.StepStatus.COMPLETED);
            step1.setDurationMs(45L);
            step1.setStartedAt(savedExec1.getStartedAt());
            step1.setCompletedAt(savedExec1.getStartedAt().plusMillis(45));
            stepRepository.save(step1);

            com.marketflow.model.ExecutionStep step2 = new com.marketflow.model.ExecutionStep(savedExec1, "demo_ai", "ai_lead_qualifier", "AI Lead Scoring");
            step2.setStatus(com.marketflow.model.enums.StepStatus.COMPLETED);
            step2.setOutputData("{\"score\":88,\"tier\":\"HOT\",\"rationale\":\"Enterprise VP with $75k+ budget\"}");
            step2.setDurationMs(210L);
            step2.setStartedAt(savedExec1.getStartedAt().plusMillis(45));
            step2.setCompletedAt(savedExec1.getStartedAt().plusMillis(255));
            stepRepository.save(step2);

            // Seed 1 Failed Edge-Case Execution
            com.marketflow.model.Execution exec2 = new com.marketflow.model.Execution(savedWf, "{\"email\":\"invalid-contact@test.com\"}");
            exec2.setStatus(com.marketflow.model.enums.ExecutionStatus.FAILED);
            exec2.setStartedAt(java.time.Instant.now().minusSeconds(1800));
            exec2.setCompletedAt(java.time.Instant.now().minusSeconds(1799));
            exec2.setErrorMessage("External notification delivery failed: 504 Gateway Timeout");
            executionRepository.save(exec2);

            log.info("Successfully seeded demo workflow and past execution records.");
        } catch (Exception ex) {
            log.warn("Could not seed demo workflow: {}", ex.getMessage());
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
