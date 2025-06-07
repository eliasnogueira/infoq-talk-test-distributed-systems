# Payment Service

This is a simple microservice that simulates a **payment processing system**, showcasing event-driven architecture using
**Apache Kafka**, **Spring Boot**, and **Testcontainers**.

## 📌 Project Overview

The **Payment Service** is responsible for:

- Creating and managing payments.
- Publishing payment events (e.g., `CREATED`, `COMPLETED`, `FAILED`) to a Kafka topic.
- Consuming those events and storing or handling them accordingly.

The service uses **Kafka** for asynchronous messaging and includes **integration tests** that verify message flow using
**Testcontainers**.

## 🧰 Technologies Used

| Technology         | Purpose                               |
|--------------------|---------------------------------------|
| **Spring Boot**    | Main application framework            |
| **Apache Kafka**   | Messaging system for event publishing |
| **Testcontainers** | Kafka container for integration tests |
| **Spring Kafka**   | Kafka integration with Spring         |
| **Awaitility**     | Handling asynchronous test assertions |
| **JUnit 5**        | Testing framework                     |
| **Lombok**         | Boilerplate code reduction            |
| **Docker Compose** | Optional local Kafka setup            |


## ▶️ Running the Application

To run the application locally, you need a Kafka broker running. You can do this in two ways:

### Option 1: Use Docker Compose (Manual Kafka Setup)

Start Kafka locally with:

```bash
docker-compose up -d
```

This will start:

* Zookeeper
* Kafka Broker
* Kafka UI (accessible at http://localhost:8088)

Make sure your` application.yml` points to `localhost:9092` as the Kafka bootstrap server.

### Option 2: Run Tests with Kafka via Testcontainers

You don't need Docker Compose for testing. The integration test class (`PaymentKafkaIntegrationTest`) uses
Testcontainers, which automatically starts a Kafka container when the tests run.

To execute tests:

```bash
./mvnw test
```

Kafka will be started and stopped automatically as part of the test lifecycle.

## 🧪 Integration Testing

The test `PaymentKafkaIntegrationTest` verifies that:

* A `PaymentEvent` is correctly published to Kafka.
* The consumer listens to the event and processes it.
* The flow works correctly even in an asynchronous environment.

Key libraries:

* `Awaitility`: used to wait until a condition is true in async scenarios.
* `Testcontainers`: spawns a real Kafka broker in Docker during tests.

## 🔄 Kafka Topic

* Topic name: `payment-events`
* Payload: `PaymentEvent` object serialized as JSON
* Producer: `PaymentKafkaProducer`
* Consumer: `PaymentKafkaConsumer`