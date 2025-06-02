package io.akka.monitoring.application;

import akka.javasdk.JsonSupport;
import akka.javasdk.agent.Agent;
import akka.javasdk.agent.ModelProvider;
import akka.javasdk.annotations.AgentDescription;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.akka.monitoring.application.AggregatedTemperatureView.LastMeasurementsQuery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ComponentId("temperature-agent")
@AgentDescription(name = "Temperature summary agent", description = "Agent that summarizes temperature data from sensors. It provides an overview of the average, minimum, and maximum temperatures recorded by sensors in a specific location.")
public class TemperatureSummaryAgent extends Agent {

  private final ComponentClient componentClient;

  public static final String SYSTEM_MESSAGE = """
    You are an AI system responsible for monitoring temperature sensors in an industrial environment. Your task is to generate a concise and informative summary based on recent measurements from three temperature sensors, each located in a different room.
    
    You are given a JSON array of sensor readings for the most recent time window and the two preceding time windows (each window is one minute apart). Each reading contains:
    - timestamp
    - sensorId
    - location
    - average, min, and max temperature readings.
    
    Up to 3 degrees Celsius is considered a normal variation in temperature readings, while anything above or below that should be considered an anomaly.
    
    Analyze the data by:
    1. Identifying general trends per location (e.g., rising or falling temperatures).
    2. Highlighting any anomalies such as sudden spikes/drops, unusually high/low values, or irregular behavior.
    3. Comparing current measurements to the previous two windows and indicating any significant changes.
    4. Mentioning if all systems are stable or if specific rooms/sensors might require attention.
    
    Write a brief summary using bulleted points per location, focusing on the most relevant insights. 
    Ensure that your summary is clear and concise, avoiding unnecessary technical jargon.
    Use the room names in your report to improve readability.
    Do not include any additional information of the overall summary.
    
    """;

  private final ModelProvider modelProvider = ModelProvider.openAi()
    .withApiKey(System.getenv("OPENAI_API_KEY"))
    .withModelName("gpt-4o");

  public TemperatureSummaryAgent(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<String> summarize() {
    Instant nowMinusMinute = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);

    var lastMeasurements = componentClient.forView()
      .method(AggregatedTemperatureView::query)
      .invoke(new LastMeasurementsQuery(nowMinusMinute));

    if (lastMeasurements.entries().isEmpty()) {
      return effects().reply("There are no temperature data available");
    }

    try {
      var userMessage = JsonSupport.getObjectMapper().writeValueAsString(lastMeasurements.entries());
      return effects()
        .model(modelProvider)
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage(userMessage)
        .responseAs(String.class)
        .thenReply();
    } catch (JsonProcessingException e) {
      return effects().error("Failed to serialize temperature entries: " + e.getMessage());
    }

  }
}
