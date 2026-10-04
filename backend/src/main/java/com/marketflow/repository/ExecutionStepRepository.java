package com.marketflow.repository;

import com.marketflow.model.ExecutionStep;
import com.marketflow.model.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionStepRepository extends JpaRepository<ExecutionStep, String> {

    List<ExecutionStep> findByExecutionIdOrderByStartedAtAsc(String executionId);

    List<ExecutionStep> findByExecutionIdAndStatus(String executionId, StepStatus status);
}
