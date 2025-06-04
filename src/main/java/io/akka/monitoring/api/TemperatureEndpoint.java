package io.akka.monitoring.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.agent.SessionMemoryEntity;
import akka.javasdk.agent.SessionMemoryEntity.GetHistoryCmd;
import akka.javasdk.agent.SessionMessage;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import io.akka.monitoring.application.AggregatedTemperature;
import io.akka.monitoring.application.AggregatedTemperatureView;
import io.akka.monitoring.application.AggregatedTemperatureView.LastMeasurementsQuery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static io.akka.monitoring.application.TemperatureSummaryAgent.AGENT_SESSION_ID;
import static java.lang.System.currentTimeMillis;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/temperatures")
public class TemperatureEndpoint {

  private final ComponentClient componentClient;

  public TemperatureEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  record Summary(long timestamp, String text) {
  }

  @Get("/current/{sensorId}")
  public AggregatedTemperature.AggregatedData get(String sensorId) {
    var now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
    var id = now.toString() + AggregatedTemperature.SEPARATOR + sensorId;
    return componentClient.forKeyValueEntity(id)
      .method(AggregatedTemperature::getAggregatedData)
      .invoke();
  }

  @Get
  public List<AggregatedTemperatureView.AggregatedTemperatureEntry> getLastMeasurements() {
    Instant nowMinusMinute = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);

    return componentClient.forView()
      .method(AggregatedTemperatureView::query)
      .invoke(new LastMeasurementsQuery(nowMinusMinute))
      .entries();
  }

  @Get("/real-time")
  public HttpResponse realTimeUpdates() {

    var temperatureUpdates = componentClient.forView()
      .stream(AggregatedTemperatureView::continuousTemperature)
      .source();

    return HttpResponses.serverSentEvents(temperatureUpdates);
  }

  @Get("/summary")
  public Summary summary() {

    var sessionMessages = componentClient.forEventSourcedEntity(AGENT_SESSION_ID)
      .method(SessionMemoryEntity::getHistory)
      .invoke(new GetHistoryCmd(Optional.empty())) //this should be last 2
      .messages();

    var aiTextResponses = sessionMessages.stream()
      .filter(message -> message instanceof SessionMessage.AiMessage)
      .map(SessionMessage.AiMessage.class::cast)
      .map(aiMessage -> new Summary(aiMessage.timestamp(), aiMessage.text()))
      .toList();

    if (aiTextResponses.isEmpty()) {
      return new Summary(currentTimeMillis(), "No summary available yet. Please wait for the temperature agent to generate a summary.");
    } else {
      return aiTextResponses.getLast();
    }
  }
}
