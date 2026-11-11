# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

The project involves many architectural trade-offs (saga style, messaging, resilience
patterns). Without written reasoning, these decisions become hard to understand later.

## Decision

We record significant architecture decisions as short Architecture Decision Records
(ADR) in `docs/adr/`, numbered sequentially, using the sections: Context, Decision,
Consequences.

## Consequences

- Decisions and their trade-offs are traceable.
- Writing an ADR adds a small overhead, kept low by keeping each record short.
