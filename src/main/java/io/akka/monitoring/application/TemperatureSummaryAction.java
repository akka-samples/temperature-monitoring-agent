package io.akka.monitoring.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.timedaction.TimedAction;
import akka.javasdk.timer.TimerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static io.akka.monitoring.Bootstrap.AGENT_SESSION_ID;

@ComponentId("temperature-summary-action")
public class TemperatureSummaryAction extends TimedAction {

  private static final Logger log = LoggerFactory.getLogger(TemperatureSummaryAction.class);
  public static final String CALL_AGENT_TIMER_NAME = "call-agent";
  private final ComponentClient componentClient;
  private final TimerScheduler scheduler;

  public TemperatureSummaryAction(ComponentClient componentClient, TimerScheduler scheduler) {
    this.componentClient = componentClient;
    this.scheduler = scheduler;
  }

  public Effect callAgent() {
    try {
      var result = componentClient.forAgent()
        .inSession(AGENT_SESSION_ID)
        .method(TemperatureSummaryAgent::summarize)
        .invoke();
      log.info("Temperature summary from agent: \n{}", result);
      // Schedule the next call to the agent after 30 seconds
      var callSummaryAgent = componentClient.forTimedAction()
        .method(TemperatureSummaryAction::callAgent)
        .deferred();

      scheduler.createSingleTimer(CALL_AGENT_TIMER_NAME,
        Duration.ofSeconds(30),
        callSummaryAgent);
      return effects().done();
    } catch (Exception e) {
      log.error("Agent call error", e);
      return effects().error(e.getMessage());
    }
  }
}
