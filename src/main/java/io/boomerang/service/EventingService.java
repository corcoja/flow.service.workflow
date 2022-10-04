package io.boomerang.service;

import java.util.concurrent.Future;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.cloudevents.CloudEvent;

public interface EventingService {

  void processCloudEventRequest(CloudEvent cloudEvent) throws Exception;

  void processNATSMessage(String payload) throws Exception;

  /**
   * This method will publish asynchronously a Cloud Event encoded as a string to the NATS server.
   * Please make sure the status of the {@link ActivityEntity} is updated when invoking this method.
   * 
   * @param activityEntity Activity entity.
   * @return A future object for publishing the new status of the {@link ActivityEntity}.
   * 
   * @Note Do not invoke this method with if the status of the {@link ActivityEntity} has not been
   *       changed, as this would result in publishing a Cloud Event with the same status multiple
   *       times.
   */
  Future<Boolean> publishStatusCloudEvent(ActivityEntity activityEntity);

  /**
   * This method will publish asynchronously a Cloud Event encoded as a string to the NATS server.
   * Please make sure the status of the {@link TaskExecutionEntity} is updated when invoking this
   * method.
   * 
   * @param taskExecutionEntity Task execution entity.
   * @return A future object for publishing the new status of the {@link TaskExecutionEntity}.
   * 
   * @Note Do not invoke this method with if the status of the {@link TaskExecutionEntity} has not
   *       been changed, as this would result in publishing a Cloud Event with the same status
   *       multiple times.
   */
  Future<Boolean> publishStatusCloudEvent(TaskExecutionEntity taskExecutionEntity);
}