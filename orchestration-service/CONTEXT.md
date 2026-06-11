# Orchestration Service Module Context

## Purpose

`orchestration-service` should document the service-side runtime responsible for orchestration concerns outside the in-server gameplay modules.

This file is a starter scaffold. It should be deepened by someone working in the module.

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [orchestration/CONTEXT.md](../orchestration/CONTEXT.md)
- [proxy/CONTEXT.md](../proxy/CONTEXT.md)

This module may share vocabulary with the rest of the project but likely has service/runtime concerns that differ from plugin modules.

## What Orchestration Service Should Own

Likely domain areas:

- orchestration runtime responsibilities
- service-side coordination and control loops
- environment, deployment, or process-facing orchestration behavior
- contracts exposed to proxy/server modules

## Questions This File Should Eventually Answer

- What responsibilities live here instead of in `orchestration`?
- Which inputs and outputs define the service contract?
- What are the key workflows, retries, and eventual-consistency expectations?
- Which failures are recoverable and which require operator intervention?

## Suggested Sections To Add Next

1. Glossary
2. Service responsibilities
3. Request/event lifecycle
4. Operational invariants
5. Integration points with proxy and orchestration
