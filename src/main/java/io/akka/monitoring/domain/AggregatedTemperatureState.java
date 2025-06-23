package io.akka.monitoring.domain;

import io.akka.monitoring.application.AggregatedTemperature;

import java.time.Instant;

public record AggregatedTemperatureState(Instant timestamp, Location location, CollectedData data) {

  public static AggregatedTemperatureState empty(Instant timestamp) {
    return new AggregatedTemperatureState(timestamp, new Location("", ""), new CollectedData(0.0, 0, Double.MAX_VALUE, Double.MIN_VALUE));
  }

  public AggregatedTemperatureState update(AggregatedTemperature.TemperatureMeasurement measurement) {
    Location location = measurement.location();
    return new AggregatedTemperatureState(timestamp, location, data.update(measurement.temperature()));
  }
}
