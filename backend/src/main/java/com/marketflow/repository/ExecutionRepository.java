package com.marketflow.repository;

import com.marketflow.model.Execution;
import com.marketflow.model.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, String> {

    List<Execution> findByWorkflowIdOrderByCreatedAtDesc(String workflowId);

    List<Execution> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(ExecutionStatus status);

    long countByWorkflowId(String workflowId);
}
