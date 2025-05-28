package io.akka.monitoring.api;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;
import io.akka.monitoring.application.AggregatedTemperature;
import io.akka.monitoring.application.AggregatedTemperatureView;
import io.akka.monitoring.application.AggregatedTemperatureView.LastMeasurementsQuery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/temperatures")
public class TemperatureEndpoint {

  private final ComponentClient componentClient;

  public TemperatureEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
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
}
