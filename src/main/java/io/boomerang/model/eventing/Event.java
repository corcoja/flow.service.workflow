package io.boomerang.model.eventing;

import java.net.URI;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import io.cloudevents.CloudEvent;

public class Event {

  protected static final String EVENT_TYPE_PREFIX = "io.boomerang.eventing.";

  private String id;

  private URI source;

  private String subject;

  private String token;

  private Date date;

  private EventType type;

  public Event() {}

  public Event(String id, URI source, String subject, String token, Date date, EventType type) {
    this.id = id;
    this.source = source;
    this.subject = subject;
    this.token = token;
    this.date = date;
    this.type = type;
  }

  public static Event fromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {
    return null;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public URI getSource() {
    return this.source;
  }

  public void setSource(URI source) {
    this.source = source;
  }

  public String getSubject() {
    return this.subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getToken() {
    return this.token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Date getDate() {
    return this.date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public EventType getType() {
    return this.type;
  }

  public void setType(EventType type) {
    this.type = type;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " id='" + getId() + "'" +
      ", source='" + getSource() + "'" +
      ", subject='" + getSubject() + "'" +
      ", token='" + getToken() + "'" +
      ", date='" + getDate() + "'" +
      ", type='" + getType() + "'" +
      "}";
  }
  // @formatter:on
}
