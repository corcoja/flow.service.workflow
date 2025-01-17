package io.boomerang.service.refactor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.Task;
import io.boomerang.model.TaskResult;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.mongo.model.WorkflowExecutionCondition;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.model.next.Dependency;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.util.GraphProcessor;

@Service
public class DAGUtility {

  @Autowired
  private ActivityTaskService taskActivityService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private RevisionService workflowVersionService;

  private List<String> calculateNodesToRemove(Graph<String, DefaultEdge> graph,
      List<Task> tasksToRun, String activityId, String value, final String currentVert,
      Task currentTask) {
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);

    List<String> matchedNodes = new LinkedList<>();
    List<String> defaultNodes = new LinkedList<>();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      Task destTask = tasksToRun.stream().filter(t -> t.getTaskId().equals(destination)).findFirst()
          .orElse(null);
      if (destTask != null) {
        determineNodeMatching(currentVert, matchedNodes, defaultNodes, value, destTask);
      }
    }
    List<String> removeList = matchedNodes;
    if (matchedNodes.isEmpty()) {
      removeList = defaultNodes;
    }
    return removeList;
  }

  public boolean canCompleteTask(ActivityEntity workflowActivity, String taskId) {
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
    List<Task> tasks = this.createTaskList(revision, workflowActivity);
    final Task start = tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getTaskType()))
        .findAny().orElse(null);
    final Task current =
        tasks.stream().filter(tsk -> taskId.equals(tsk.getTaskId())).findAny().orElse(null);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks, workflowActivity);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getTaskId());
    return (pathFromStart.getPath(current.getTaskId()) != null);
  }

  private Graph<String, DefaultEdge> createGraph(List<Task> tasks) {
    final List<String> vertices = tasks.stream().map(Task::getTaskId).collect(Collectors.toList());

    final List<Pair<String, String>> edgeList = new LinkedList<>();
    for (final Task task : tasks) {
      for (final String dep : task.getDependencies()) {
        final Pair<String, String> pair = Pair.of(dep, task.getTaskId());
        edgeList.add(pair);
      }
    }
    return GraphProcessor.createGraph(vertices, edgeList);
  }

  private Graph<String, DefaultEdge> createGraph(List<Task> tasks, ActivityEntity activity) {
    Graph<String, DefaultEdge> graph = createGraph(tasks);
    TopologicalOrderIterator<String, DefaultEdge> orderIterator =
        new TopologicalOrderIterator<>(graph);
    while (orderIterator.hasNext()) {

      final String taskId = orderIterator.next();
      Task currentTask = this.getTaskByid(tasks, taskId);
      if (TaskType.start != currentTask.getTaskType()
          && TaskType.end != currentTask.getTaskType()) {
        if (currentTask.getTaskActivityId() == null) {
          continue;
        }

        TaskExecutionEntity taskExecution =
            taskActivityService.findById(currentTask.getTaskActivityId());
       
        TaskStatus flowTaskStatus = taskExecution.getFlowTaskStatus();
        if (flowTaskStatus == TaskStatus.completed || flowTaskStatus == TaskStatus.failure) {
          if (currentTask.getTaskType() == TaskType.decision) {
            String switchValue = taskExecution.getSwitchValue();
            processDecision(graph, tasks, activity.getId(), switchValue, currentTask.getTaskId(),
                currentTask);

          } else {
            TaskResult result = new TaskResult();
            result.setStatus(flowTaskStatus);
            this.updateTaskInGraph(result, graph, tasks, taskId);
          }
        }
      }
    }

    return graph;
  }

  private List<Task> createTaskList(RevisionEntity revisionEntity, ActivityEntity activity) {

    final Dag dag = revisionEntity.getDag();

    final List<Task> taskList = new LinkedList<>();
    for (final DAGTask dagTask : dag.getTasks()) {

      final Task newTask = new Task();
      newTask.setTaskId(dagTask.getTaskId());
      newTask.setTaskType(dagTask.getType());
      newTask.setTaskName(dagTask.getLabel());

      TaskExecutionEntity task =
          taskActivityService.findByTaskIdAndActivityId(dagTask.getTaskId(), activity.getId());
      if (task != null) {
        newTask.setTaskActivityId(task.getId());
      }

      final String workFlowId = revisionEntity.getWorkFlowId();
      newTask.setWorkflowId(workFlowId);

      if (dagTask.getType() == TaskType.script || dagTask.getType() == TaskType.template || dagTask.getType() == TaskType.customtask) {
        String templateId = dagTask.getTemplateId();
        final FlowTaskTemplateEntity flowTaskTemplate =
            templateService.getTaskTemplateWithId(templateId);
        newTask.setTemplateId(flowTaskTemplate.getId());
       
        Integer templateVersion = dagTask.getTemplateVersion();
        List<Revision> revisions = flowTaskTemplate.getRevisions();
        if (revisions != null) {
          Optional<Revision> result = revisions.stream().parallel()
              .filter(revision -> revision.getVersion().equals(templateVersion)).findAny();
          if (result.isPresent()) {
            Revision revision = result.get();
            newTask.setRevision(revision);
            newTask.setResults(revision.getResults());
          } else {
            Optional<Revision> latestRevision = revisions.stream()
                .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
            if (latestRevision.isPresent()) {
              newTask.setRevision(latestRevision.get());
              newTask.setResults(newTask.getRevision().getResults());
            }
          }
        } else {
          throw new IllegalArgumentException("Invalid task template selected: " + templateId);

        }

        Map<String, String> properties = new HashMap<>();
        if (dagTask.getProperties() != null) {
          for (KeyValuePair property : dagTask.getProperties()) {
            properties.put(property.getKey(), property.getValue());
          }
        }
        newTask.setInputs(properties);
        if (newTask.getResults() == null) {
          newTask.setResults(dagTask.getResults());
        }
      } else if (dagTask.getType() == TaskType.decision) {
        newTask.setDecisionValue(dagTask.getDecisionValue());
      }

      final List<String> taskDepedancies = new LinkedList<>();
      for (Dependency dependency : dagTask.getDependencies()) {
        taskDepedancies.add(dependency.getTaskId());
      }
      newTask.setDetailedDepednacies(dagTask.getDependencies());
      newTask.setDependencies(taskDepedancies);
      taskList.add(newTask);
    }
    return taskList;
  }

  private void determineNodeMatching(final String currentVert, List<String> matchedNodes,
      List<String> defaultNodes, String value, Task destTask) {
    Optional<Dependency> optionalDependency = destTask.getDetailedDepednacies().stream()
        .filter(d -> d.getTaskId().equals(currentVert)).findAny();
    if (optionalDependency.isPresent()) {
      Dependency dependency = optionalDependency.get();
      String linkValue = dependency.getSwitchCondition();

      String node = destTask.getTaskId();

      boolean matched = false;

      if (linkValue != null) {
        String[] lines = linkValue.split("\\r?\\n");
        for (String line : lines) {
          String patternString = line;
          Pattern pattern = Pattern.compile(patternString);
          Matcher matcher = pattern.matcher(value);
          if (matcher.matches()) {
            matched = true;
          }
        }
        if (matched) {
          matchedNodes.add(node);
        }
      } else {
        defaultNodes.add(node);
      }
    }
  }

  private void determineNodeMatching(final String currentVert, List<String> matchedNodes,
      TaskStatus status, Task destTask) {
    Optional<Dependency> optionalDependency = destTask.getDetailedDepednacies().stream()
        .filter(d -> d.getTaskId().equals(currentVert)).findAny();
    if (optionalDependency.isPresent()) {
      Dependency dependency = optionalDependency.get();
      WorkflowExecutionCondition condition = dependency.getExecutionCondition();
      String node = destTask.getTaskId();
      if (condition != null
          && (status == TaskStatus.failure && condition == WorkflowExecutionCondition.failure)
          || (status == TaskStatus.completed && condition == WorkflowExecutionCondition.success)
          || (condition == WorkflowExecutionCondition.always)) {
        matchedNodes.add(node);
      }
    }
  }

  private Task getTaskByid(List<Task> tasks, String id) {
    return tasks.stream().filter(tsk -> id.equals(tsk.getTaskId())).findAny().orElse(null);
  }

  private void processDecision(Graph<String, DefaultEdge> graph, List<Task> tasksToRun,
      String activityId, String value, final String currentVertex, Task currentTask) {
    List<String> removeList =
        calculateNodesToRemove(graph, tasksToRun, activityId, value, currentVertex, currentTask);
    Iterator<DefaultEdge> itrerator = graph.edgesOf(currentVertex).iterator();
    while (itrerator.hasNext()) {
      DefaultEdge e = itrerator.next();
      String destination = graph.getEdgeTarget(e);
      String source = graph.getEdgeSource(e);

      if (source.equals(currentVertex)
          && removeList.stream().noneMatch(str -> str.trim().equals(destination))) {
        graph.removeEdge(e);
      }
    }
  }

  private void updateTaskInGraph(TaskResult result, Graph<String, DefaultEdge> graph,
      List<Task> tasksToRun, String currentVert) {
    List<String> matchedNodes = new LinkedList<>();
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);
    TaskStatus value = result.getStatus();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      Task destTask = tasksToRun.stream().filter(t -> t.getTaskId().equals(destination)).findFirst()
          .orElse(null);
      if (destTask != null) {
        determineNodeMatching(currentVert, matchedNodes, value, destTask);
      }
    }

    Iterator<DefaultEdge> itrerator = graph.edgesOf(currentVert).iterator();
    while (itrerator.hasNext()) {
      DefaultEdge e = itrerator.next();
      String destination = graph.getEdgeTarget(e);
      String source = graph.getEdgeSource(e);
      if (source.equals(currentVert)
          && matchedNodes.stream().noneMatch(str -> str.trim().equals(destination))) {
        graph.removeEdge(e);
      }
    }
  }

  public boolean validateWorkflow(ActivityEntity workflowActivity) {
    
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
    List<Task> tasks = this.createTaskList(revision, workflowActivity);
    final Task start = tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getTaskType()))
        .findAny().orElse(null);
    final Task end =
        tasks.stream().filter(tsk -> TaskType.end.equals(tsk.getTaskType())).findAny().orElse(null);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks, workflowActivity);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getTaskId());
    return (pathFromStart.getPath(end.getTaskId()) != null);
  }
}
