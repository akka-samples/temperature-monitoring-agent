package io.akka.monitoring.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import io.akka.monitoring.domain.AggregatedTemperatureState;

import java.time.Instant;
import java.util.List;

@ComponentId("aggregated-temperature-view")
public class AggregatedTemperatureView extends View {

  public record AggregatedTemperatureEntry(
    Instant timestamp,
    String sensorId,
    String location,
    double averageTemperature,
    double minTemperature,
    double maxTemperature
  ) {
  }

  public record AggregatedTemperatureEntries(List<AggregatedTemperatureEntry> entries) {
  }

  @Consume.FromKeyValueEntity(AggregatedTemperature.class)
  public static class AggregatedTemperatureUpdater extends TableUpdater<AggregatedTemperatureEntry> {

    public Effect<AggregatedTemperatureEntry> update(AggregatedTemperatureState update) {

      AggregatedTemperatureEntry entry = new AggregatedTemperatureEntry(
        update.timestamp(),
        update.location().sensorId(),
        update.location().location(),
        update.data().averageTemperature(),
        update.data().minTemperature(),
        update.data().maxTemperature()
      );
      return effects().updateRow(entry);
    }
  }

  public record LastMeasurementsQuery(Instant timestamp) {
  }

  @Query("select * as entries from temperatures where timestamp <= :timestamp order by timestamp desc limit 3")
  public QueryEffect<AggregatedTemperatureEntries> query(LastMeasurementsQuery lastMeasurementsQuery) {
    return queryResult();
  }

  @Query("select * as entries from temperatures  order by timestamp desc limit 3")
  public QueryEffect<AggregatedTemperatureEntries> queryLatest() {
    return queryResult();
  }

  @Query(value = "select * from temperatures", streamUpdates = true)
  public QueryStreamEffect<AggregatedTemperatureEntry> continuousTemperature() {
    return queryStreamResult();
  }
}
