package io.boomerang.mongo.model;

public class Triggers {

  private Trigger manual;
  private TriggerScheduler scheduler;
  private TriggerEvent webhook;
  private TriggerEvent custom;
  
  public Triggers() {
    webhook = new TriggerEvent();
    custom = new TriggerEvent();
    manual = new TriggerEvent();
    scheduler = new TriggerScheduler();
    
    manual.setEnable(Boolean.TRUE);
  }
  
  
  public Trigger getManual() {
    return manual;
  }
  public void setManual(Trigger manual) {
    this.manual = manual;
  }
  public TriggerScheduler getScheduler() {
    return scheduler;
  }
  public void setScheduler(TriggerScheduler scheduler) {
    this.scheduler = scheduler;
  }
  public TriggerEvent getWebhook() {
    return webhook;
  }
  public void setWebhook(TriggerEvent webhook) {
    this.webhook = webhook;
  }
  public TriggerEvent getCustom() {
    return custom;
  }
  public void setCustom(TriggerEvent custom) {
    this.custom = custom;
  }
}
