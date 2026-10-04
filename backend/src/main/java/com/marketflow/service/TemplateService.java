package com.marketflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.*;
import com.marketflow.exception.TemplateNotFoundException;
import com.marketflow.model.Workflow;
import com.marketflow.model.WorkflowTemplate;
import com.marketflow.model.enums.WorkflowStatus;
import com.marketflow.repository.WorkflowRepository;
import com.marketflow.repository.WorkflowTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;

    public TemplateService(WorkflowTemplateRepository templateRepository,
                           WorkflowRepository workflowRepository,
                           ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.workflowRepository = workflowRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<WorkflowTemplateDto> listTemplates(String category) {
        List<WorkflowTemplate> templates;
        if (category != null && !category.isBlank()) {
            templates = templateRepository.findByCategoryOrderByNameAsc(category);
        } else {
            templates = templateRepository.findAllByOrderByNameAsc();
        }

        return templates.stream()
                .map(this::toTemplateDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowTemplateDto getTemplate(String id) {
        WorkflowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found with id: " + id));
        return toTemplateDto(template);
    }

    @Transactional
    public WorkflowDetailResponse instantiateTemplate(String templateId, String customName) {
        log.info("Instantiating workflow from template [{}]", templateId);
        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found with id: " + templateId));

        String workflowName = (customName != null && !customName.isBlank()) ? customName : template.getName();
        Workflow workflow = new Workflow(workflowName, template.getDescription(), template.getDefinition());
        workflow.setStatus(WorkflowStatus.ACTIVE);
        Workflow saved = workflowRepository.save(workflow);

        WorkflowGraphDto graph = parseGraph(saved.getDefinition());
        return new WorkflowDetailResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getStatus(),
                graph.getNodes(),
                graph.getEdges()
        );
    }

    private WorkflowTemplateDto toTemplateDto(WorkflowTemplate template) {
        WorkflowGraphDto graph = parseGraph(template.getDefinition());
        return new WorkflowTemplateDto(
                template.getId(),
                template.getName(),
                template.getCategory(),
                template.getDescription(),
                graph.getNodes(),
                graph.getEdges(),
                template.getCreatedAt()
        );
    }

    private WorkflowGraphDto parseGraph(String definition) {
        if (definition == null || definition.isBlank()) {
            return new WorkflowGraphDto();
        }
        try {
            return objectMapper.readValue(definition, WorkflowGraphDto.class);
        } catch (Exception e) {
            try {
                Map<String, Object> map = objectMapper.readValue(definition, new TypeReference<>() {});
                WorkflowGraphDto graph = new WorkflowGraphDto();
                if (map.containsKey("nodes")) {
                    graph.setNodes(objectMapper.convertValue(map.get("nodes"), new TypeReference<List<NodeDto>>() {}));
                }
                if (map.containsKey("edges")) {
                    graph.setEdges(objectMapper.convertValue(map.get("edges"), new TypeReference<List<EdgeDto>>() {}));
                }
                return graph;
            } catch (Exception ex) {
                return new WorkflowGraphDto();
            }
        }
    }
}
