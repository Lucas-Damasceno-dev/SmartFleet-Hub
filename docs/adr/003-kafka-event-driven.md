# 003 - Event-Driven Architecture with Kafka

* **Status:** Accepted
* **Date:** 2025-01-16
* **Deciders:** Architecture Team

## Context
Microservices need to communicate asynchronously for:
- GPS telemetry ingestion at high volume
- Order state changes propagation
- Notification triggers
- Analytics and reporting pipelines

## Decision
We will use **Apache Kafka** as our event streaming platform with **Debezium** for CDC because:
1. Proven scalability for high-throughput messaging
2. Durable event log for replay and recovery
3. Debezium enables Transactional Outbox pattern without dual-write issues
4. Strong ecosystem (Kafka Connect, Streams)
5. Supports multiple consumer groups for different use cases

## Consequences
* **Positive:** Decoupled services with reliable event delivery
* **Positive:** Built-in event replay for debugging and recovery
* **Positive:** Horizontal scaling for increased load
* **Negative:** Additional infrastructure complexity (Zookeeper/KRaft)
* **Negative:** Eventual consistency requires careful handling
