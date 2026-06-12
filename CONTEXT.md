# BetterPvP Context

## Purpose

BetterPvP is a Minecraft server project with a large shared `core` and multiple gameplay systems built around persistent player state.

At a high level, the project manages:

- Player identity and account-like state
- Realm-scoped gameplay state
- Stats and rewards
- Punishments and moderation workflows
- Live gameplay presentation and interaction

This document is not a full code map. It is the shared domain model and architectural context that should help humans and agents reason about the system without guessing.

## How To Use This Document

- Start here when you need project language before changing code.
- Prefer the terms in this document over inventing new ones.
- Add new terms only when they represent a real domain concept, not just a class name.
- Record rules and invariants here before documenting implementation detail elsewhere.

## Module Map

The repo is split across major project submodules. This root file is the map; each large module should keep its own local context file for deeper rules and flows.

### Foundational Module

- [core/CONTEXT.md](core/CONTEXT.md) - shared player model, persistence, gameplay infrastructure, and cross-cutting systems

### Major Gameplay Modules

- [champions/CONTEXT.md](champions/CONTEXT.md) - champion-driven combat/gameplay systems
- [clans/CONTEXT.md](clans/CONTEXT.md) - clan/social/group state and flows
- [game/CONTEXT.md](game/CONTEXT.md) - game-mode or game-loop orchestration
- [hub/CONTEXT.md](hub/CONTEXT.md) - hub/lobby-specific player experience and flows
- [lunar/CONTEXT.md](lunar/CONTEXT.md) - Lunar Client-facing integrations and behavior
- [progression/CONTEXT.md](progression/CONTEXT.md) - progression systems, milestones, and long-term advancement
- [shops/CONTEXT.md](shops/CONTEXT.md) - shop, purchasing, and catalog behavior

### Platform And Infrastructure Modules

- [orchestration/CONTEXT.md](orchestration/CONTEXT.md) - orchestration-side coordination logic and contracts
- [orchestration-service/CONTEXT.md](orchestration-service/CONTEXT.md) - orchestration service runtime, responsibilities, and integration rules
- [proxy/CONTEXT.md](proxy/CONTEXT.md) - proxy-side player/session/network behavior

### Private Modules

- [private/compatability/CONTEXT.md](private/compatability/CONTEXT.md) - compatibility glue for private integrations or version-specific behavior
- [private/events/CONTEXT.md](private/events/CONTEXT.md) - private event-driven gameplay/content flows
- [private/dungeons/CONTEXT.md](private/dungeons/CONTEXT.md) - dungeon-specific content and progression
- [private/store/CONTEXT.md](private/store/CONTEXT.md) - private store integrations and store-owned domain rules
- [private/store-proxy/CONTEXT.md](private/store-proxy/CONTEXT.md) - proxy-side store integration and cross-process store coordination

### Suggested Ownership Rule

- Put shared language in the root file.
- Put foundational shared mechanics in `core/CONTEXT.md`.
- Put module-local flows, invariants, and integration rules in each module's own `CONTEXT.md`.
- Avoid redefining `Client`, `Gamer`, `Realm`, and other shared concepts in every module unless the module constrains them in a module-specific way.

## Ubiquitous Language

### Client

A `Client` is the main persistent player identity in the system.

- It owns cross-realm or player-wide state such as rank, punishments, ignores, and client properties.
- It contains a `Gamer`.
- It also owns a `StatContainer`.
- A `Client` may be loaded in memory even when the player is offline.
- A `Client` is not the same thing as a Bukkit `Player`.

### Gamer

A `Gamer` represents the player's realm-scoped gameplay state.

- The source comment in [Gamer.java](core/src/main/java/me/mykindos/betterpvp/core/client/gamer/Gamer.java) describes it as a client's seasonal data.
- It owns gameplay-facing properties like balance, fragments, PvP protection, combat timing, movement timing, and UI/display state.
- It is where many live gameplay interactions hang off once a player is online.
- A `Gamer` is not the persistent identity; that role belongs to `Client`.

### Realm

A `Realm` is an execution context for realm-scoped data.

- It has an `id`.
- It belongs to a `Server`.
- It belongs to a `Season`.
- Gamer properties and many stats are scoped to the current realm.

### Rank

A `Rank` describes privilege and presentation level for a `Client`.

- It affects permissions, moderation bypasses, server access rules, and tag display.
- Rank checks are commonly hierarchical, for example `client.hasRank(...)`.

### Property

A property is persisted key-value state stored in a `PropertyContainer`.

- `ClientProperty` represents client-scoped properties.
- `GamerProperty` represents gamer-scoped properties.
- Property updates fire events when values change in memory.
- Many defaults are applied lazily during load or join flows rather than being guaranteed at object construction time.

### Stat

A stat is tracked gameplay progress or measurement attached to a `Client` through its `StatContainer`.

- Stats are frequently mutated in memory during gameplay.
- Persistence is batched or flushed later rather than on every mutation.
- Many stats are realm-scoped.

### Punishment

A `Punishment` is a moderation record attached to a client.

- It has a type, rule, apply time, optional expiry, optional revocation, and actor metadata.
- A punishment can be active, expired, or revoked.
- Punishments are used both for enforcement and for historical display.

### RewardBox

A `RewardBox` is serialized reward storage associated with a client and season-like progression.

- It is loaded and saved through the client persistence path.
- It is a domain concept, not just a transport format.

### Loaded

Loaded means a `Client` is present in in-memory storage.

- Loaded does not necessarily mean online.
- Offline lookups may load a client into memory without making them online.
- Cache expiry can later unload a client if they are not online.

### Online

Online means the Bukkit `Player` is present and available.

- `Client.isLoaded()` currently checks Bukkit presence rather than cache presence.
- Many call sites assume `search().online(...)` returns a fully available gameplay object graph.

## Core Flows

### Login / Client Load Lifecycle

This is one of the most important flows in the project.

Current shape:

1. Pre-login loads or creates the `Client`.
2. Additional client data is assembled from persistence.
3. Pre-load and load events are fired.
4. Join-time defaults and join-time side effects are applied.
5. The player is treated as online.

Important notes:

- Client data may be created on first login.
- Loading currently spans listeners, cache management, and SQL-backed assembly.
- Join-time property/default checks still exist after the main load phase.

### Logout / Unload Lifecycle

When a player leaves:

1. Quit events update time-based properties.
2. Property and stat changes are flushed asynchronously.
3. The client may be removed from in-memory storage if the player is offline.

Important notes:

- Unload is not identical to save.
- A client may survive temporarily in memory after going offline.
- Cache expiry is part of the lifecycle, not just an optimization detail.

### Property Load / Save / Flush Lifecycle

Properties exist at two main scopes: client and gamer.

Current shape:

1. Properties are loaded from persistence during client assembly.
2. Property changes happen in memory through property containers.
3. Property change events fire on mutation.
4. SQL queries are queued rather than executed immediately.
5. Queued property updates are flushed later in batches.

Important notes:

- Client and gamer properties are persisted separately.
- Gamer properties depend on the current realm.
- Defaults are sometimes applied after load if missing.

### Stat Mutation / Persistence Lifecycle

Stats are primarily gameplay-facing mutable state.

Current shape:

1. Gameplay code increments or mutates stats through the loaded client.
2. Dirty stat state accumulates in memory.
3. Periodic or quit-time flushes persist changed stats.

Important notes:

- Stats may be disabled by configuration.
- Disabling stat persistence does not necessarily remove in-memory stat behavior.
- Stat loading and stat saving are separate concerns and happen at different times.

### Punishment Lifecycle

Punishments are moderation records attached to clients.

Current shape:

1. Punishments are loaded as part of client assembly.
2. Gameplay and moderation code check active punishments when making decisions.
3. Punishments may expire or be revoked later.
4. Historical views may need additional client lookups for actor names.

Important notes:

- A punishment is not just a boolean flag.
- Expiry and revocation both matter to correctness.

## Module Responsibilities

These are the responsibilities that appear central today. They are intentionally phrased at the domain level, not as a full package inventory.

### `core.client`

Owns the main player state model.

- `Client`
- `Rank`
- client-scoped properties
- client events

### `core.client.gamer`

Owns realm-scoped gameplay state and player-facing display/runtime state.

- balance and fragments
- combat/movement timing
- chat channel state
- sidebars, boss bars, action bars, title queues

### `core.client.repository`

Owns client loading, storage, lookup, and persistence coordination.

- in-memory loaded/offline lookup
- SQL-backed client assembly
- cache expiry and unload behavior
- queued property/stat persistence

This area is especially important because many other modules depend on it directly.

### `core.client.stats`

Owns stat definitions, stat storage shape, stat loading, and stat persistence behavior.

### `core.client.punishments`

Owns moderation records, rules, punishment types, and punishment application/history behavior.

### `core.client.rewards`

Owns reward storage and reward serialization/deserialization.

### `core.command`, `core.chat`, `core.combat`, and other gameplay modules

These modules mostly consume loaded client or gamer state to implement gameplay and moderation features.

## Key Invariants

- A `Client` contains a `Gamer`.
- A `Client` may be loaded while offline.
- A `Gamer` is not a standalone account model; it is tied to a client and current realm behavior.
- Client properties and gamer properties are different scopes and should not be casually mixed.
- Many gamer properties are only meaningful in the current realm.
- Rank is attached to `Client`, not `Gamer`.
- Punishments are attached to `Client`, not to a transient player session.
- Stats are mutated in memory first and often persisted later.
- Defaults for some properties are applied lazily, so "missing property" is a real state during part of the lifecycle.

## Architectural Tensions

These are useful to know before changing code.

### Loaded vs Online Is Easy To Confuse

The codebase uses both concepts heavily, and they are not interchangeable.

- Loaded is about in-memory presence.
- Online is about Bukkit player presence.
- Bugs are likely when code assumes one implies the other in every path.

### `ClientManager` Is A Very Wide Seam

`ClientManager` is the central lookup point for much of the codebase.

- This is convenient.
- It also means many unrelated concerns can accumulate there.
- Changes in client lifecycle or persistence can ripple broadly if the seam grows further.

### Persistence Is Split Across Immediate State And Deferred Flushes

The codebase often updates in-memory objects immediately and persistence later.

- This improves gameplay responsiveness.
- It also means debugging requires knowing when state is authoritative in memory versus in storage.

### Defaults Are Applied In More Than One Place

Some domain defaults are not guaranteed by construction alone.

- They may be applied during load.
- They may be applied during join listeners.
- This makes lifecycle order important.

## Known Good Starting Points For Future Documentation

If this file needs deepening, these are the next best additions:

1. A dedicated section on login/load/unload event ordering
2. A dedicated section on property scopes and defaulting rules
3. A dedicated section on stat categories and realm scoping
4. A dedicated section on punishment rules and actor semantics
5. Cross-module interaction rules between `core`, `champions`, `clans`, `game`, `shops`, and private modules

## Open Questions

These are good candidates to resolve as the document evolves.

- Which concepts are truly cross-realm versus only currently implemented that way?
- Should `loaded` and `online` become explicitly documented project-wide terms in code comments and APIs?
- Which property defaults are required invariants versus migration/backfill behavior?
- Which parts of client persistence are intentionally batched, and which are legacy?

## Non-Goals

This document does not try to:

- List every package or class
- Explain every listener
- Describe every SQL table
- Replace ADRs or design notes for individual subsystems

It exists to help contributors reason about the project with consistent language.
