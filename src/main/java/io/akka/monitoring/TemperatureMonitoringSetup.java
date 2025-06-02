package io.akka.monitoring;

import akka.Done;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import io.akka.monitoring.application.IoTDeviceTemperatureStream;
import io.akka.monitoring.application.TemperatureSummaryAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.TimeUnit.SECONDS;


@Setup
public class TemperatureMonitoringSetup implements ServiceSetup {

  private static final Logger log = LoggerFactory.getLogger(TemperatureMonitoringSetup.class);
  private final Materializer materializer;
  private final ComponentClient componentClient;
  private final IoTDeviceTemperatureStream temperatureStream;
  private CompletionStage<Done> runningStream;
  public static final String AGENT_SESSION_ID = "temperature-monitoring-session";

  public TemperatureMonitoringSetup(Materializer materializer, ComponentClient componentClient) {
    this.materializer = materializer;
    this.componentClient = componentClient;
    this.temperatureStream = new IoTDeviceTemperatureStream(componentClient);
  }

  @Override
  public void onStartup() {
    //run the temperature stream that will simulate IoT device temperature readings
    this.runningStream = temperatureStream
      .createStream()
      .runWith(Sink.ignore(), materializer);

    //schedule the agent call to summarize the temperature readings
    materializer.scheduleAtFixedRate(
      FiniteDuration.apply(10, SECONDS),
      FiniteDuration.apply(30, SECONDS),
      this::callAgent);
  }

  private void callAgent() {
    try {
      var result = componentClient.forAgent()
        .inSession(AGENT_SESSION_ID)
        .method(TemperatureSummaryAgent::summarize)
        .invoke();
      log.info("Temperature summary from agent: \n{}", result);
    } catch (Exception e) {
      log.error("Agent call error", e);
    }
  }
}
