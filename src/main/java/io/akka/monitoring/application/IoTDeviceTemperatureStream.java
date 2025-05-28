package io.akka.monitoring.application;

import akka.Done;
import akka.NotUsed;
import akka.javasdk.client.ComponentClient;
import akka.stream.javadsl.Source;
import io.akka.monitoring.domain.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Simulates a stream of temperature measurements from IoT devices located in different rooms.
 * It feeds the system with both historical data (last 10 minutes) and real-time measurements.
 *
 * In real-world applications, this fake steam would be replaced with a {@link akka.javasdk.consumer.Consumer}
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
      return timestamp.truncatedTo(ChronoUnit.MINUTES).toString() + AggregatedTemperature.SEPARATOR + location.sensorId();
    }

    public RawTemperatureMeasurement withTimestamp(Instant newTimestamp) {
      return new RawTemperatureMeasurement(location, temperature, newTimestamp);
    }
  }

  public Source<Done, NotUsed> createStream() {

    return historicalDataSource().concat(Source.tick(
        java.time.Duration.ofSeconds(1),
        java.time.Duration.ofSeconds(1),
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
      .method(AggregatedTemperature::record)
      .invoke(new AggregatedTemperature.TemperatureMeasurement(measurement.location, measurement.temperature));
    return Done.done();
  }

  private Source<Done, NotUsed> historicalDataSource() {
    var now = Instant.now();
    return Source.range(10, 1, -1)
      .flatMapConcat(i -> Source.from(LOCATIONS)
        .map(location ->
          randomTemperatureMeasurement(location).withTimestamp(now.truncatedTo(ChronoUnit.MINUTES).minus(i, ChronoUnit.MINUTES))
        ))
      .map(this::updateAggregation);
  }

  public RawTemperatureMeasurement randomTemperatureMeasurement(Location location) {
    return new RawTemperatureMeasurement(location,
      randomDoubleInRange(20.0, 30.0),
      Instant.now());
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

    // Round to 2 decimal places
    BigDecimal bd = new BigDecimal(rawValue).setScale(2, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }
}
