package io.boomerang.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.projectstormv5.RestConfig;
import io.boomerang.mongo.model.ChangeLog;
import io.boomerang.mongo.model.Dag;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows_revisions')}")
public class RevisionEntity {

  private Dag dag;

  @Id
  private String id;

  private long version;

  private String workFlowId;

  private ChangeLog changelog;

  private String markdown;
  
  private RestConfig config;

  public Dag getDag() {
    return dag;
  }

  public String getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  public String getWorkFlowId() {
    return workFlowId;
  }

  public void setDag(Dag dag) {
    this.dag = dag;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void setWorkFlowId(String workFlowId) {
    this.workFlowId = workFlowId;
  }

  public ChangeLog getChangelog() {
    return changelog;
  }

  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
  }

  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }

  public RestConfig getConfig() {
    return config;
  }

  public void setConfig(RestConfig config) {
    this.config = config;
  }
}
