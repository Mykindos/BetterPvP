# Proxy Module Context

## Purpose

`proxy` should document proxy-side player, routing, and session behavior for the BetterPvP network.

This file is a starter scaffold. It should be deepened by someone working in the module.

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [core/CONTEXT.md](../core/CONTEXT.md)

The proxy layer likely coordinates with shared player identity and rank concepts, but it may have different runtime constraints than in-server modules.

## What Proxy Should Own

Likely domain areas:

- cross-server player routing
- proxy-side session management
- permissions or access checks at the network edge
- message passing or forwarding between network layers
- network-level lifecycle that exists before a player reaches a server module

## Questions This File Should Eventually Answer

- What is authoritative at the proxy layer versus at server modules?
- Which player/session concepts exist only on the proxy?
- How are handoff, connect, disconnect, and fallback flows handled?
- How does proxy integrate with orchestration and store-proxy behavior?

## Suggested Sections To Add Next

1. Glossary
2. Session and routing lifecycle
3. Cross-process communication rules
4. Integration points with orchestration and core concepts
5. Failure/reconnect behavior
