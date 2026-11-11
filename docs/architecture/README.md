# Architecture

> **Status: target architecture (planned).** This diagram shows the intended design,
> not what is currently implemented.

```mermaid
flowchart LR
    Client[React UI / k6] --> GW[API Gateway]
    GW --> CAT[Product Catalog]
    GW --> ORD[Order Service]
    ORD -- "REST + timeout + circuit breaker" --> PAY[Payment Service]
    ORD <-- "events" --> MQ[(RabbitMQ)]
    INV[Inventory Service] <--> MQ
    PAY <--> MQ
    SHP[Shipping Service] <--> MQ
    NTF[Notification Service] <--> MQ
    RL[ResilienceLab Backend] -. "load tests / fault injection" .-> GW
    RLUI[ResilienceLab UI] --> RL
```

## Principles

- One database per service; no cross-service database access.
- Saga orchestrated by the Order Service.
- Transactional Outbox for reliable event publishing.
- Idempotent consumers and idempotent payment processing.

Design decisions are recorded in [docs/adr](../adr/).
