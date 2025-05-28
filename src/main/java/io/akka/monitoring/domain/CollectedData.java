package io.akka.monitoring.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CollectedData(double totalTemperature, int totalCount, double minTemperature, double maxTemperature) {
  public CollectedData update(double temperature) {
    double newTotalTemperature = totalTemperature + temperature;
    int newTotalCount = totalCount + 1;
    double newMinTemperature = Math.min(minTemperature, temperature);
    double newMaxTemperature = Math.max(maxTemperature, temperature);
    return new CollectedData(newTotalTemperature, newTotalCount, newMinTemperature, newMaxTemperature);
  }

  public double averageTemperature() {
    return totalCount > 0
      ? averageRounded()
      : 0.0;
  }

  private double averageRounded() {
    // Round to 2 decimal places
    BigDecimal bd = new BigDecimal(totalTemperature / totalCount).setScale(2, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }
}
