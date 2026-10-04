package com.marketflow.repository;

import com.marketflow.model.Execution;
import com.marketflow.model.ExecutionStep;
import com.marketflow.model.Workflow;
import com.marketflow.model.WorkflowTemplate;
import com.marketflow.model.enums.ExecutionStatus;
import com.marketflow.model.enums.StepStatus;
import com.marketflow.model.enums.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RepositoryIntegrationTests {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionStepRepository executionStepRepository;

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    @Test
    @DisplayName("Should successfully persist and retrieve Workflow")
    void testSaveAndRetrieveWorkflow() {
        Workflow workflow = new Workflow("Instagram Lead Qualification", "Qualify leads from Instagram", "{\"nodes\":[],\"edges\":[]}");
        workflow.setStatus(WorkflowStatus.ACTIVE);
        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("wf_"));
        assertEquals("Instagram Lead Qualification", saved.getName());
        assertEquals(WorkflowStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<Workflow> found = workflowRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Instagram Lead Qualification", found.get().getName());
    }

    @Test
    @DisplayName("Should save execution and execution steps with cascade")
    void testSaveExecutionWithSteps() {
        Workflow workflow = workflowRepository.save(new Workflow("E-Commerce Abandoned Cart", "Recover carts", "{\"nodes\":[],\"edges\":[]"));

        Execution execution = new Execution(workflow, "{\"cartId\": 1234, \"email\": \"user@example.com\"}");
        execution.setStatus(ExecutionStatus.RUNNING);

        ExecutionStep step1 = new ExecutionStep(execution, "node_1", "trigger", "Cart Abandoned Trigger");
        step1.setStatus(StepStatus.COMPLETED);
        step1.setOutputData("{\"cartTotal\": 99.0}");

        ExecutionStep step2 = new ExecutionStep(execution, "node_2", "action", "Send Discount Email");
        step2.setStatus(StepStatus.RUNNING);

        execution.addStep(step1);
        execution.addStep(step2);

        Execution saved = executionRepository.save(execution);

        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("exec_"));
        assertEquals(2, saved.getSteps().size());

        List<Execution> runs = executionRepository.findByWorkflowIdOrderByCreatedAtDesc(workflow.getId());
        assertEquals(1, runs.size());
        assertEquals(ExecutionStatus.RUNNING, runs.get(0).getStatus());

        List<ExecutionStep> steps = executionStepRepository.findByExecutionIdOrderByStartedAtAsc(saved.getId());
        assertEquals(2, steps.size());
        assertEquals("node_1", steps.get(0).getNodeId());
        assertEquals(StepStatus.COMPLETED, steps.get(0).getStatus());
    }

    @Test
    @DisplayName("Should persist and search workflow templates by category")
    void testWorkflowTemplates() {
        WorkflowTemplate template1 = new WorkflowTemplate(
                "Lead Qualification",
                "Marketing",
                "Automatically qualify inbound leads",
                "{\"nodes\":[]}"
        );
        templateRepository.save(template1);

        List<WorkflowTemplate> marketingTemplates = templateRepository.findByCategoryOrderByNameAsc("Marketing");
        assertFalse(marketingTemplates.isEmpty());
        assertEquals("Lead Qualification", marketingTemplates.get(0).getName());
    }
}
