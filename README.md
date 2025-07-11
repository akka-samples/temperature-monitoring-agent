# Temperature Monitoring Agent

A temperature monitoring system that collects, aggregates, and analyzes temperature data from IoT sensors. The 
system uses AI to generate insights about temperature trends and anomalies across different locations.

It is built using Akka components like:
 - HTTP Endpoint,
 - Key Value Entity,
 - Timed Action,
 - View,
 - and the most recent one - an Agent component that streamlines the interaction with LLM.

## Features

- Simulates IoT temperature sensors in three locations (Boiler Room A, Server Room B, Warehouse C)
- Collects and aggregates temperature data in minute-based time windows
- Calculates average, minimum, and maximum temperatures for each location
- Uses OpenAI's GPT-4o model to analyze temperature patterns and detect anomalies
- Provides REST API endpoints to access current and historical temperature data


## Getting Started

### Prerequisites

- Java 21 installed
- Apache Maven
- Docker (for deployment)
- OpenAI API key

### Building the Project

Use Maven to build the project:

```shell
mvn compile
```

### Running Locally

Export the necessary environment variables for the Akka service:

```shell
export OPENAI_API_KEY="your_openai_api_key"
```

Or change the `application.conf` file to use a different model provider.

To start the service locally, run:

```shell
mvn compile exec:java
```

Open your browser and navigate to `http://localhost:9000` to access the service.

## Deployment

You can use the [Akka Console](https://console.akka.io) to create a project and deploy this service. Once you have a project created, follow these steps.

For more information on deployment, refer to [Deploy and manage services](https://doc.akka.io/operations/services/deploy-service.html).

To understand the Akka concepts that are the basis for this example, see [Development Process](https://doc.akka.io/concepts/development-process.html) in the documentation.
