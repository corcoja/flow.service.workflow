package io.boomerang.controller.api;

import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.extensions.SlackExtension;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1/extensions")
@Tag(name = "Extensions Management",
    description = "Specific use cases via extensions.")
public class ExtensionsV1Controller {

  private static final Logger LOGGER = LogManager.getLogger();
  @Autowired
  private SlackExtension slackExtension;

  @PostMapping(value = "/slack/commands", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Receive Slack Slash Commands")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackCommand(HttpServletRequest request, 
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam MultiValueMap<String, String> slackEvent) throws JsonMappingException, JsonProcessingException {
    LOGGER.debug(slackEvent);
    CompletableFuture.supplyAsync(slackExtension.createRunModal(slackEvent.get("trigger_id").get(0), slackEvent.get("user_id").get(0), slackEvent.get("text").get(0)));
    return ResponseEntity.ok().build();
  }
  
  @PostMapping(value = "/slack/interactivity", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Receive Slack Interactivity")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackInteractivity(HttpServletRequest request,
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam MultiValueMap<String, String> slackEvent) throws JsonMappingException, JsonProcessingException {
    LOGGER.debug(slackEvent);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode payload = mapper.readTree(slackEvent.get("payload").get(0));
    if (payload.has("type") && "view_submission".equals(payload.get("type").asText())) {
      CompletableFuture.supplyAsync(slackExtension.executeRunModal(payload));
    } else if (payload.has("type")) {
      LOGGER.error("Unhandled Slack Interactivity Type: " + payload.get("type").asText());
    } else {
      LOGGER.error("Unhandled Slack Interactivity Payload with no Type: " + payload.toPrettyString());
    }
    return ResponseEntity.ok().build();
  }
  
  @GetMapping(value = "/slack/auth")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Receive Slack Oauth2 request")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackAuth(HttpServletRequest request,
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam String code) throws JsonMappingException, JsonProcessingException {
    return slackExtension.handleAuth(code);
  }
  
//  TODO: integrate with Slack events.
//  @PostMapping(value = "/slack/events", consumes = {MediaType.APPLICATION_JSON_VALUE})
//  @AuthenticationScope(scopes = {TokenScope.global})
//  @Operation(summary = "Receive Slack Events")
//  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
//      @ApiResponse(responseCode = "400", description = "Bad Request")})
//  ResponseEntity<?> receiveSlackEvent(HttpServletRequest request, 
//      @RequestHeader("x-slack-request-timestamp") String timestamp,
//      @RequestHeader("x-slack-signature") String signature,
//      @RequestBody JsonNode slackEvent) throws JsonMappingException, JsonProcessingException {
//    if (slackEvent.has("challenge")) {
//      LOGGER.info("Challenge: " + slackEvent.get("challenge"));
//      return ResponseEntity.ok().body(slackEvent.get("challenge"));
//    }
//    LOGGER.info(slackEvent);
//    return ResponseEntity.ok().build();
//  }
}
