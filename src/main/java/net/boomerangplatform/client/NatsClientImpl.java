package net.boomerangplatform.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.AttributesImpl;
import io.cloudevents.v1.http.Unmarshallers;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.service.FlowWorkflowService;

@Component
public class NatsClientImpl implements NatsClient {

	@Value("${eventing.nats.url}")
	private String natsUrl;

	@Value("${eventing.nats.cluster}")
	private String natsCluster;

    protected static final String SUBJECT = "flow-workflow-execute";

    protected static final String QUEUE = "flow-workflow";

    protected static final String TYPE_PREFIX = "io.boomerang.eventing.";

	private StreamingConnection streamingConnection;

	private final Logger logger = LogManager.getLogger();

    @Autowired
    private FlowWorkflowService workflowService;

	private void processMessage(String payload) {  
	  Map<String, Object> httpHeaders = new HashMap<>();
	  httpHeaders.put("Content-Type", "application/cloudevents+json");
	  
	  CloudEvent<AttributesImpl, Map> event =
	        Unmarshallers.structured(Map.class)
	            .withHeaders(() -> httpHeaders)
	            .withPayload(() -> payload)
	            .unmarshal();
	  
	  logger.info("Process Message - Attributes: " + event.getAttributes().toString());
	  logger.info("Process Message - Payload: " + event.getData().toString());
	  
//	  TODO determine the trigger implementation
	  String workflowId = event.getAttributes().getSubject().orElse("");
	  String trigger = event.getAttributes().getType().replace(TYPE_PREFIX, "");
	  if (trigger.equals(workflowService.getWorkflow(workflowId).getTriggers().getEvent().getTopic())) {
	    logger.info("Process Message - Trigger(" + trigger + ") is allowed.");
	    
        List<FlowProperty> properties = workflowService.getWorkflow(workflowId).getProperties();
        if (properties != null) {
          properties.forEach(FlowProperty -> {
            JsonNode propertyValue = JsonPath.parse(payload).read("$"+FlowProperty.getKey(), JsonNode.class);
            logger.info("Process Message - Property Found: key=" + FlowProperty.getKey() + ",value=" + propertyValue);
          });
        }
	  }
	  
	}

//	TODO IF eventing enabled, start this on application startup OR is this what @EventListener is doing?
	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) throws TimeoutException {

		logger.info("Initializng subscriptions to NATS");

		int random = (int) (Math.random() * 10000 + 1); // NOSONAR

        Options cfOptions = new Options.Builder().natsUrl(natsUrl).clusterId(natsCluster).clientId("flow-workflow-" + random).build();
        StreamingConnectionFactory cf = new StreamingConnectionFactory(cfOptions);
        
        try {
          this.streamingConnection = cf.createConnection();

          Subscription subscription =
              streamingConnection.subscribe(SUBJECT, QUEUE, new MessageHandler() { // NOSONAR
                @Override
                public void onMessage(Message m) {
                  processMessage(new String(m.getData()));
                }
              }, new SubscriptionOptions.Builder().durableName("durable").build());
        } catch (IOException ex) {
          logger.error(ex);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
//        TODO do we close connection and subscription?
	}

}
