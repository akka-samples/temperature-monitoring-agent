package io.akka.monitoring.application;

import akka.Done;
import akka.NotUsed;
import akka.javasdk.client.ComponentClient;
import akka.stream.javadsl.Source;
import io.akka.monitoring.domain.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import static java.time.Duration.ofSeconds;

/**
 * Simulates a stream of temperature measurements from IoT devices located in different rooms.
 * It feeds the system with both historical data (last 10 minutes) and real-time measurements.
 * <p>
 * In real-world applications, this fake stream would be replaced with a {@link akka.javasdk.consumer.Consumer}
 * that reads from a real-time data source.
 */
public class IoTDeviceTemperatureStream {

  private static final Logger log = LoggerFactory.getLogger(IoTDeviceTemperatureStream.class);
  private final ComponentClient componentClient;
  private final Random random = new Random();
  public final List<Location> LOCATIONS = List.of(
    new Location("temp-101", "Boiler Room A"),
    new Location("temp-102", "Server Room B"),
    new Location("temp-103", "Warehouse C"));

  public IoTDeviceTemperatureStream(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  record RawTemperatureMeasurement(
    Location location,
    double temperature,
    Instant timestamp
  ) {
    public String aggregationId() {
      return timestamp.truncatedTo(ChronoUnit.MINUTES).toString() + AggregatedTemperatureEntity.SEPARATOR + location.sensorId();
    }

    public RawTemperatureMeasurement withTimestamp(Instant newTimestamp) {
      return new RawTemperatureMeasurement(location, temperature, newTimestamp);
    }
  }

  public Source<Done, NotUsed> createStream() {

    return historicalDataSource().concatLazy(Source.tick(
        ofSeconds(1),
        ofSeconds(1),
        "tick"
      )
      .mapMaterializedValue(__ -> NotUsed.getInstance())
      .map(__ -> randomTemperatureMeasurement(randomLocation()))
      .map(this::updateAggregation));
  }

  private Done updateAggregation(RawTemperatureMeasurement measurement) {
    var aggregationId = measurement.aggregationId();
    log.info("{} new measurement: {}", aggregationId, measurement);
    componentClient.forKeyValueEntity(aggregationId)
      .method(AggregatedTemperatureEntity::record)
      .invoke(new AggregatedTemperatureEntity.TemperatureMeasurement(measurement.location, measurement.temperature));
    return Done.done();
  }

  private Source<Done, NotUsed> historicalDataSource() {
    var now = Instant.now();
    return Source.range(3, 1, -1)
      .flatMapConcat(i -> Source.from(LOCATIONS)
        .map(location ->
          randomTemperatureMeasurement(location).withTimestamp(now.truncatedTo(ChronoUnit.MINUTES).minus(i, ChronoUnit.MINUTES))
        ))
      .map(this::updateAggregation);
  }

  public RawTemperatureMeasurement randomTemperatureMeasurement(Location location) {
    return new RawTemperatureMeasurement(location,
      randomTemperature(location),
      Instant.now());
  }

  private double randomTemperature(Location location) {
    if (location.sensorId().equals("temp-101")) {
      // Boiler Room A is expected to be hotter
      return randomDoubleInRange(30.0, 40.0);
    } else if (location.sensorId().equals("temp-103")) {
      // Warehouse C is expected to be cooler
      return randomDoubleInRange(10.0, 20.0);
    } else {
      return randomDoubleInRange(20.0, 30.0);
    }
  }

  private Location randomLocation() {
    int locationIndex = random.nextInt(LOCATIONS.size());
    return LOCATIONS.get(locationIndex);
  }

  private double randomDoubleInRange(double min, double max) {
    if (min >= max) {
      throw new IllegalArgumentException("max must be greater than min");
    }
    double rawValue = min + (max - min) * random.nextDouble();

    return Math.round(rawValue * 10.0) / 10.0;
  }
}
