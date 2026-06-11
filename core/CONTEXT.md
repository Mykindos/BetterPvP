# Core Module Context

## Purpose

`core` is the foundational module for the BetterPvP project.

It provides the shared player model, persistence model, gameplay infrastructure, event-driven runtime behavior, and utility systems that other modules build on.

If another module needs to reason about player identity, gamer state, rank, stats, punishments, shared commands, chat, combat helpers, or persistence, it will usually depend on `core`.

## What Core Owns

`core` is the home of the shared concepts that should remain stable across the rest of the repo.

Primary owned areas include:

- `Client`
- `Gamer`
- `Rank`
- `Realm`
- client and gamer properties
- stats and stat persistence
- punishments and moderation records
- rewards
- search/load/unload flows for player state
- shared command, chat, combat, and presentation infrastructure

## Relationship To Root Context

Read the root [CONTEXT.md](../CONTEXT.md) first for project-wide language.

This file should go deeper on:

- foundational flows inside `core`
- invariants that other modules rely on
- shared runtime and persistence assumptions

This file should not try to fully document every downstream module.

## Core Subdomains

### Player State

The most important shared model is the `Client` / `Gamer` split.

- `Client` is the persistent player identity.
- `Gamer` is the realm-scoped gameplay state attached to that client.
- Many gameplay modules consume `Gamer` state through a loaded `Client`.

### Persistence

`core` owns the main load, cache, flush, and persistence coordination path for player state.

- Clients can be loaded while offline.
- Property updates are often deferred and flushed later.
- Stat updates are commonly buffered in memory before persistence.

### Runtime Infrastructure

`core` also provides runtime patterns that many modules rely on:

- listeners
- events
- commands
- scheduled updates
- display/UI helpers
- search helpers over player state

## High-Value Flows To Document Next

These are likely the most useful next expansions for this file:

1. Client load / unload event ordering
2. Property scopes and defaulting rules
3. Stat mutation and flush rules
4. Punishment enforcement and history flows
5. Combat-state ownership and timing rules

## Invariants Other Modules Should Assume

- Shared player identity comes from `Client`, not from module-local wrappers.
- Realm-scoped gameplay state hangs off `Gamer`.
- Rank and punishments are shared cross-module concepts.
- Property and stat persistence behavior originates in `core`, even when mutations happen elsewhere.
- Modules should prefer depending on `core` language rather than inventing parallel player models.

## Integration Guidance

When another module adds behavior around players:

- extend shared concepts only when the concept is truly project-wide
- keep module-local rules in the owning module
- avoid making `core` depend back on leaf gameplay modules
