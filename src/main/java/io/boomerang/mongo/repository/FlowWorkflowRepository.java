package io.boomerang.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.WorkflowStatus;

public interface FlowWorkflowRepository extends MongoRepository<WorkflowEntity, String> {

  List<WorkflowEntity> findByFlowTeamId(String flowTeamId);

  List<WorkflowEntity> findByFlowTeamIdIn(List<String> flowTeamIds);

  @Query("{ 'tokens.token' : ?0 }")
  WorkflowEntity findByToken(String tokenString);

  @Query("{ 'triggers.scheduler.enable' : true }")
  List<WorkflowEntity> findAllScheduledWorkflows();

  @Query("{ 'triggers.event.enable' : true }")
  List<WorkflowEntity> findAllEventWorkflows();

  @Query("{ 'triggers.event.enable' : true, 'triggers.event.topic' : ?0 }")
  List<WorkflowEntity> findAllEventWorkflowsForTopic(String topic);

  List<WorkflowEntity> findByScope(WorkflowScope system);

  List<WorkflowEntity> findByScopeAndStatusIn(WorkflowScope system, List<String> list);

  List<WorkflowEntity> findByScopeAndTriggersIn(WorkflowScope system, List<String> list);

  List<WorkflowEntity> findByScopeAndStatusInAndTriggersIn(WorkflowScope system, List<String> list,
      List<String> list2);

  List<WorkflowEntity> findByScopeAndOwnerUserIdAndStatus(WorkflowScope user, String id,
      WorkflowStatus active);

  List<WorkflowEntity> findByScopeAndStatus(WorkflowScope template, WorkflowStatus active);
}
