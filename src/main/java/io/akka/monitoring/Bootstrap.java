package io.akka.monitoring;

import akka.Done;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.timer.TimerScheduler;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import io.akka.monitoring.application.IoTDeviceTemperatureStream;
import io.akka.monitoring.application.TemperatureSummaryAction;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static io.akka.monitoring.application.TemperatureSummaryAction.CALL_AGENT_TIMER_NAME;


@Setup
public class Bootstrap implements ServiceSetup {

  private final Materializer materializer;
  private final ComponentClient componentClient;
  private final IoTDeviceTemperatureStream temperatureStream;
  private final TimerScheduler timerScheduler;
  private CompletionStage<Done> runningStream;

  public Bootstrap(Materializer materializer, ComponentClient componentClient, TimerScheduler timerScheduler) {
    this.materializer = materializer;
    this.componentClient = componentClient;
    this.temperatureStream = new IoTDeviceTemperatureStream(componentClient);
    this.timerScheduler = timerScheduler;
  }

  @Override
  public void onStartup() {
    //run the temperature stream that will simulate IoT device temperature readings
    //this is a fake stream, in a real application it would be replaced with a consumer reading from a real-time data source
    //or with an Endpoint that receives real-time data from IoT devices
    this.runningStream = temperatureStream
      .createStream()
      .runWith(Sink.ignore(), materializer);

    //schedule the agent call to summarize the temperature readings
    timerScheduler.createSingleTimer(CALL_AGENT_TIMER_NAME,
      Duration.ofSeconds(10),
      componentClient.forTimedAction()
        .method(TemperatureSummaryAction::callAgent)
        .deferred());
  }
}
