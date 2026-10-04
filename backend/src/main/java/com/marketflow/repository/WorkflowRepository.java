package com.marketflow.repository;

import com.marketflow.model.Workflow;
import com.marketflow.model.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, String> {

    List<Workflow> findAllByOrderByUpdatedAtDesc();

    List<Workflow> findByStatusOrderByUpdatedAtDesc(WorkflowStatus status);

    List<Workflow> findByUserIdOrderByUpdatedAtDesc(String userId);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Workflow w WHERE w.userId = :userId OR w.userId IS NULL ORDER BY w.updatedAt DESC")
    List<Workflow> findAccessibleWorkflows(@org.springframework.data.repository.query.Param("userId") String userId);

    long countByStatus(WorkflowStatus status);
}
