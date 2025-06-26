package io.akka.monitoring.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import akka.javasdk.keyvalueentity.KeyValueEntityContext;
import io.akka.monitoring.domain.AggregatedTemperatureState;
import io.akka.monitoring.domain.Location;

import java.time.Instant;

import static akka.Done.done;

@ComponentId("aggregated-temperature")
public class AggregatedTemperatureEntity extends KeyValueEntity<AggregatedTemperatureState> {

  public static final String SEPARATOR = ";";
  private final KeyValueEntityContext context;

  public AggregatedTemperatureEntity(KeyValueEntityContext context) {
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
}
