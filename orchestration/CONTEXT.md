# Orchestration Module Context

## Purpose

`orchestration` should document the domain model and coordination contracts for orchestration behavior across the project.

This file is a starter scaffold. It should be deepened by someone working in the module.

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [proxy/CONTEXT.md](../proxy/CONTEXT.md)
- [orchestration-service/CONTEXT.md](../orchestration-service/CONTEXT.md)

## What Orchestration Should Own

Likely domain areas:

- coordination contracts between network/runtime pieces
- orchestration-side data models
- command or event shapes used for control-plane behavior
- lifecycle coordination across servers or processes

## Questions This File Should Eventually Answer

- What is orchestrated in BetterPvP terms?
- Which concepts are transport-level contracts versus business/domain concepts?
- How are orchestration commands/events produced, consumed, retried, or reconciled?
- Where is the seam between library/shared-contract code and service/runtime code?

## Suggested Sections To Add Next

1. Ubiquitous language
2. Contract model
3. Main orchestration flows
4. Ownership split with orchestration-service
5. Compatibility/versioning rules
