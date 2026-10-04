package com.marketflow.repository;

import com.marketflow.model.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, String> {

    List<WorkflowTemplate> findByCategoryOrderByNameAsc(String category);

    List<WorkflowTemplate> findAllByOrderByNameAsc();
}
