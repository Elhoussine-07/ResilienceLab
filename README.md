# ResilienceLab

**Distributed Systems Reliability & Performance Engineering Platform**

> **Status: under active development.** This repository is being built step by step.
> Nothing below should be read as already implemented unless it is checked in the
> [Roadmap](#roadmap).

## Description

ResilienceLab is a platform to configure, run and analyse reliability and performance
experiments on a distributed system. The first version targets its own e-commerce
microservices system, used as a test environment.

## Problem

Distributed systems fail in partial, unpredictable ways: slow dependencies, duplicated
messages, crashed consumers, unavailable brokers. These behaviours are rarely tested
systematically, so teams discover them in production.

## Proposed solution

Two parts:

- **Part A: E-commerce distributed system (test target).** Six microservices
  (Product Catalog, Order, Inventory, Payment, Shipping, Notification) communicating over
  REST and RabbitMQ, implementing a Saga, Transactional Outbox and idempotent processing.
- **Part B: ResilienceLab platform.** A modular backend and a React UI to create
  experiments, run load tests, inject faults, collect metrics and compare behaviour
  before and after failures.

## Planned architecture

See [docs/architecture](docs/architecture/README.md). The diagram describes the _target_
architecture; it is updated as components are actually built.

## Planned technology stack

| Area             | Technologies                                                                |
| ---------------- | --------------------------------------------------------------------------- |
| Backend          | Java 21, Spring Boot 3, Spring Data JPA, Spring Cloud Gateway, Resilience4j |
| Messaging        | RabbitMQ (Spring AMQP)                                                      |
| Database         | PostgreSQL                                                                  |
| Frontend         | React, TypeScript                                                           |
| Testing          | JUnit 5, Mockito, Testcontainers, k6, Toxiproxy                             |
| Observability    | Prometheus, Grafana, OpenTelemetry, Jaeger                                  |
| DevOps           | Docker, Docker Compose, GitHub Actions, Trivy, JaCoCo                       |
| Cloud (optional) | AWS EC2, Terraform                                                          |

_This section lists the planned stack and will be updated to reflect what is actually used._

## Features

None completed yet. See the Roadmap.

## Prerequisites

- JDK 21
- Maven 3.9+ (or the Maven wrapper included in each service)
- Docker with Docker Compose v2
- Node.js (LTS)
- Git

## Installation, running and tests

Not available yet. These sections will be completed as the project progresses.

## Screenshots

To be added.

## Roadmap

- [ ] Repository setup and documentation structure
- [ ] Docker Compose infrastructure (PostgreSQL, RabbitMQ)
- [ ] E-commerce microservices and order workflow
- [ ] Saga, Transactional Outbox, idempotency, retries and DLQ
- [ ] Resilience4j (timeout, circuit breaker, bulkhead)
- [ ] Load testing (k6) and fault injection (Toxiproxy)
- [ ] Observability (Prometheus, Grafana, OpenTelemetry, Jaeger)
- [ ] ResilienceLab backend and React dashboard
- [ ] CI/CD pipeline
- [ ] Cloud deployment documentation

## Safety notice

Fault injection and load testing features must only be used against systems you own or
are explicitly authorized to test. The first version targets only this project's own
test environment.

## Author

**EL HOSSNI LAHOUSSINE**, engineering student at ENSA Agadir
GitHub: [@<ton-username>](https://github.com/Elhoussine-07)

## License

Released under the [MIT License](LICENSE).
