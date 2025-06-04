package io.akka.monitoring.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.timer.TimerScheduler;
import akka.javasdk.workflow.Workflow;

import java.time.Duration;

import static akka.Done.done;

@ComponentId("temperature-summary-workflow")
public class TemperatureSummaryWorkflow extends Workflow<String> {

  private final TimerScheduler scheduler;
  private final ComponentClient componentClient;

  public TemperatureSummaryWorkflow(TimerScheduler scheduler, ComponentClient componentClient) {
    this.scheduler = scheduler;
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowDef<String> definition() {
    return workflow();
  }

  public Effect<Done> start() {

    // to avoid multiple calls to the agent, we delete any existing timer
    // this helps to ensure that only one timer is active at a time
    // and prevents overlapping calls when service is deployed on multiple instances
    scheduler.delete("call-agent");

    var callSummaryAgent = componentClient.forTimedAction()
      .method(TemperatureSummaryAction::callAgent)
      .deferred();

    scheduler.createSingleTimer("call-agent",
      Duration.ofSeconds(15),
      callSummaryAgent);

    return effects().reply(done());
  }

}
