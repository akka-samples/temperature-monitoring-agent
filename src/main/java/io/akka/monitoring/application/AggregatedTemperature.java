package io.akka.monitoring.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import akka.javasdk.keyvalueentity.KeyValueEntityContext;
import io.akka.monitoring.domain.AggregatedTemperatureState;
import io.akka.monitoring.domain.CollectedData;
import io.akka.monitoring.domain.Location;

import java.time.Instant;

import static akka.Done.done;

@ComponentId("aggregated-temperature")
public class AggregatedTemperature extends KeyValueEntity<AggregatedTemperatureState> {

  public static final String SEPARATOR = ";";
  private final KeyValueEntityContext context;

  public AggregatedTemperature(KeyValueEntityContext context) {
    this.context = context;
  }


  public record TemperatureMeasurement(
    Location location,
    double temperature
  ) {
  }

  @Override
  public AggregatedTemperatureState emptyState() {
    var timestamp = context.entityId().split(SEPARATOR)[0];
    return AggregatedTemperatureState.empty(Instant.parse(timestamp));
  }

  public Effect<Done> record(TemperatureMeasurement measurement) {
    AggregatedTemperatureState updated = currentState().update(measurement);
    return effects()
      .updateState(updated)
      .thenReply(done());
  }

  public record AggregatedData(Location location, double averageTemperature, double minTemperature,
                               double maxTemperature) {
  }

  public ReadOnlyEffect<AggregatedData> getAggregatedData() {
    if (currentState().location().sensorId().isEmpty()) {
      return effects().error("temperature data not found for this timeslot");
    } else {
      CollectedData collectedData = currentState().data();
      AggregatedData aggregatedData = new AggregatedData(currentState().location(),
        collectedData.averageTemperature(),
        collectedData.minTemperature(),
        collectedData.maxTemperature());
      return effects().reply(aggregatedData);
    }
  }
}
