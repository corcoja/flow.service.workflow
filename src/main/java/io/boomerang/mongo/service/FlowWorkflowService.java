package io.boomerang.mongo.service;

import java.util.List;
import io.boomerang.mongo.entity.WorkflowEntity;

public interface FlowWorkflowService {

  void deleteWorkflow(String id);

  WorkflowEntity getWorkflow(String id);

  List<WorkflowEntity> getAllWorkflows();
  
  List<WorkflowEntity> getWorkflowsForTeam(String flowId);

  List<WorkflowEntity> getWorkflowsForTeams(List<String> flowTeamIds);

  List<WorkflowEntity> getScheduledWorkflows();

  List<WorkflowEntity> getEventWorkflows();

  List<WorkflowEntity> getEventWorkflowsForTopic(String topic);

  WorkflowEntity saveWorkflow(WorkflowEntity entity);

  WorkflowEntity findByTokenString(String tokenString);

  List<WorkflowEntity> getSystemWorkflows();
  List<WorkflowEntity> getTeamWorkflows();
  List<WorkflowEntity> getTemplateWorkflows();
  List<WorkflowEntity> getWorkflowsForUser(String id);

  List<WorkflowEntity> getUserWorkflows();


}
