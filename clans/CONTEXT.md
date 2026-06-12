# Clans Module Context

## Purpose

`clans` is a territory-faction module.

Its main job is not just to group players socially. It gives groups durable shared state, territorial control, a defendable base, clan-owned resources, and conflict systems that shape world interaction.

At a high level, Clans owns:

- clan identity and membership
- clan territory and claims
- clan relationships with other clans
- clan-owned resources and progression
- clan core, mailbox, vault, and related base systems
- pillage, protection, and territorial conflict rules
- clan-specific world and map behavior

Social grouping is part of the module, but it primarily exists in support of territorial control and long-lived faction play.

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [core/CONTEXT.md](../core/CONTEXT.md)

This module builds on shared concepts from `core`:

- `Client` remains the shared player identity.
- `Gamer` remains the player's realm-scoped gameplay state.
- `Rank`, shared stats, and generic persistence patterns remain owned by `core`.

This file does not redefine those concepts. It describes how Clans uses them.

## Ubiquitous Language

### Clan

A `Clan` is the main group-owned entity in the module.

- It has a durable identity, name, and ID.
- It owns members, relationships, territory, and clan-scoped properties.
- It owns a `ClanCore`.
- It owns clan-level resources such as bank balance, energy, points, and dominance-related state.
- It may outlive any individual player session.

### ClanMember

A `ClanMember` is a player's membership record inside a clan.

- Membership is player-linked by UUID.
- Membership includes a clan-local rank and cached client name.
- Membership is not the same thing as the shared `Client` model from `core`.

### Member Ranks / Leadership

Clan membership has a local hierarchy:

- `RECRUIT`
- `MEMBER`
- `ADMIN`
- `LEADER`

These ranks govern authority inside the clan rather than global project permissions.

Typical responsibilities implied by the module:

- leaders are the top authority for clan ownership decisions
- admins can manage substantial clan behavior
- recruits and members participate with fewer management powers

### ClanCore

The `ClanCore` is the physical and logical center of a clan's base.

- It has a location.
- It anchors attached systems like vault and mailbox.
- It participates in defense and destruction flows.
- It is part of what makes a clan feel territorial rather than just social.

### Clan Territory / Claims

Clan territory is the set of claimed chunks owned by a clan.

- Claims are chunk-based.
- Territory affects control, map presentation, interaction rules, and who "owns" a location.
- Claim capacity is rule-driven and config-driven, not arbitrary.

### Clan Bank / Balance

The clan bank is shared clan-owned money.

- It is not the same thing as an individual player balance.
- Deposits and withdrawals are clan-governed actions.
- Clan economy shows up in attached systems like auctionhouse delivery and clan-owned storage flows.
- In the current module shape, bank balance is mostly a record of what the clan has earned from auctionhouse activity rather than a top-tier strategic lever.

### Energy

Energy is a clan-owned resource used by the faction gameplay loop.

- It is accumulated by clan activity and configured systems.
- It is capped.
- It participates in defense/progression-style behavior and user-facing notifications.
- It is a clan property, not a player property.
- In practice, energy mainly dictates how long a clan can remain inactive before disband pressure becomes relevant.

### Dominance

Dominance is a clan-versus-clan standing or power metric used by the module's conflict systems.

- Dominance behavior is configurable.
- It affects conflict behavior such as pillage/TNT gating.
- It is meaningful at the clan level, not the player level.
- This is the module's main strategic gameplay metric.
- Clans ideally want positive dominance against all other clans.
- Reaching strong positive dominance can create pillage opportunities and point gains.
- Reaching strong negative dominance can create pillage vulnerability and point loss.

### Pillage

Pillage is the module's active territorial conflict/raid state.

- A clan can be pillaging or being pillaged.
- Pillage has duration, protection, and recovery-related rules.
- Pillage changes what other systems allow, such as parts of auctionhouse or territory behavior.

### Clan Relations

Clans have explicit relationship states with one another.

The key current relation concepts are:

- `ALLY`
- `ALLY_TRUST`
- `ENEMY`
- `NEUTRAL`
- `SELF`
- `SAFE`
- `PILLAGE`

These relations affect:

- map colors and presentation
- interaction expectations
- chat and social affordances
- territorial and conflict behavior

### Admin Clan

An admin clan is a first-class category of clan in this module.

- It is not just a permission bit on a normal clan.
- Admin clans affect map behavior, territory interpretation, and special-case logic.
- They should be treated as domain concepts when describing clan behavior.

### Safe Clan

A safe clan is a first-class category of clan territory/identity.

- Safe behavior is part of the domain model, not just UI wording.
- Safe status changes relation outcomes and interaction expectations.

## Primary Clan Lifecycle And Main Flows

This is the main loop contributors should have in mind when changing the module.

### 1. Clan Creation And Persistent Load

The module boots by loading clans from repository-backed storage.

- Clan records are loaded for the current realm.
- Metadata such as banner, vault, and mailbox are assembled.
- Clan properties are loaded separately.
- Members, alliances, enemies, territory, and other attached state are then used to form the in-memory clan model.

New clans enter the same long-lived lifecycle:

- create clan identity
- save initial clan and metadata
- attach members
- begin participating in territory and conflict systems

### 2. Membership Lifecycle

Membership is the first active gameplay flow after creation.

Core transitions include:

- invite
- join
- leave
- kick
- promote
- demote
- disband

Important shape:

- membership is clan-local state, not generic player identity
- leadership matters because many clan actions are rank-gated
- a player's clan association is often cached on online-player metadata for faster lookup
- offline clan lookup can fall back to scanning loaded clan objects by membership

### 3. Territory Lifecycle

A clan's territory is its main world-facing footprint.

Core transitions include:

- claim chunk
- unclaim chunk
- inspect territory ownership
- update territory presentation on maps and displays

Important shape:

- claims are finite and config-driven
- clan territory affects who controls a location
- territory is coupled to relations, world rules, and navigation affordances
- territory is one of the main reasons the module should be understood as faction gameplay rather than only social grouping

### 4. Core / Base Lifecycle

Every serious clan can develop a base centered around its core.

Core transitions include:

- set core location
- use or defend base-adjacent systems
- maintain vault and mailbox state
- respond to core destruction or raid pressure

Important shape:

- the core is where several clan-owned systems converge
- base systems are group-owned and persist beyond any one online member

### 5. Clan Resource Lifecycle

Clans accumulate and spend shared resources.

Important resource concepts include:

- bank balance
- energy
- points
- experience / level-like progression
- insurance and protection-related state

Typical flow:

- gain resources from module-owned or integrated gameplay
- persist updates as clan properties or repository-managed data
- spend or consume resources through clan actions and attached systems

Important framing:

- dominance is the main strategic gameplay lever
- bank balance is more of an earned clan-economy store than a top-tier strategic driver
- energy is mainly an upkeep/inactivity pressure system
- points are an outcome metric more than a direct resource players actively manage
- insurance and protection are recovery/defensive systems rather than player-steered strategy resources

### 6. Conflict Lifecycle

Conflict is a core gameplay loop of the module.

Key transitions include:

- change relations with other clans
- enter enemy / ally / trusted ally states
- enter pillage conditions
- defend territory and base systems
- move through protection or recovery windows

Important shape:

- pillage is not just a message state; it changes real behavior
- dominance, protection, TNT, and insurance all interact with conflict
- relationship state influences how the same world location is interpreted

### 7. Communication And Navigation Around Clan State

Once membership and relations exist, players need shared clan-facing affordances.

Main current affordances include:

- clan chat
- alliance chat
- territory-aware map/minimap behavior
- transport/navigation affordances connected to clan state

These support the main faction loop rather than replacing it.

## State Model And Ownership

### What `core` Owns

`core` owns the shared player model and project-wide infrastructure:

- `Client`
- `Gamer`
- shared rank model
- shared stats model
- generic property containers
- generic persistence patterns
- shared events, commands, and gameplay infrastructure

### What `clans` Owns

`clans` owns group-level faction state:

- clan identity
- membership and member ranks
- clan territory
- clan relations
- clan-owned resources
- clan core / vault / mailbox
- clan-specific map and world-control rules
- pillage, dominance, and territorial conflict behavior

### Important Ownership Boundaries

- Clan state is group-owned, not player-owned.
- Player state and clan state are linked, but not interchangeable.
- A player can leave, but the clan still exists.
- A clan can have world state, stored resources, and social relationships independent of any current online player.
- Clan persistence is repository-backed and separate from the `ClientManager` lifecycle in `core`.

### Persistence Shape

The module uses repository-backed persistence for clan data.

Important current patterns:

- clans load from repository storage during module startup
- clan metadata and properties are loaded separately
- property writes are queued and flushed later
- moment-to-moment gameplay mutation and durable persistence are related but not identical phases

## Integration Points

### Integration With `core`

Clans depends heavily on `core` for:

- `Client` and `Gamer` lookups
- shared stats
- shared command/event/runtime infrastructure
- shared property mapping and database abstractions

### Integration With Player Runtime

The module uses player metadata and runtime presence to speed up clan lookup.

- online players often carry cached clan metadata
- offline membership may require searching loaded clan objects

This is important when changing any code that assumes clan lookup is cheap or universally available.

### Integration With Combat And World Rules

Clans is deeply world-facing.

It integrates with:

- combat and kill flows
- TNT/protection rules
- doors, blocks, trees, sponge, and related world interaction behavior
- territory interaction and chunk ownership behavior

### Integration With Shared Stats / Rewards / Progression

The module integrates outward rather than owning the shared progression model.

Examples visible in the current module shape:

- clan-related stats
- reward listeners
- progression adapters and perk listeners

These integrations should be described as Clans using shared systems, not replacing them.

Important current note:

- progression is opportunistic integration rather than foundational clan identity
- one explicit integration point is location-based drop-table modification via `@PluginAdapter`

## Attached Subsystems

These are current and important, but they are attached to the main faction loop rather than defining the primary identity of the module.

### Fields

`fields` is a resource/control subsystem attached to Clans.

- It appears to own custom ore and interactable field behavior.
- It should be described as supporting territorial/resource gameplay, not as the main definition of a clan.

### Map / Minimap

The map subsystem visualizes clan state.

- territory ownership
- relationship colors
- point-of-interest rendering
- player and location captions

It is a representation layer over clan state, not the source of truth for it.

### Vault / Mailbox

Vault and mailbox are base-adjacent storage systems attached to the clan core.

- They are meaningful and persistent.
- They are not separate top-level identity systems; they are clan infrastructure.

### Auctionhouse Integration

Clans integrates with auction flows through clan-owned delivery and restrictions.

- Clan membership and territory can gate auction behavior.
- Auction outcomes can pay into clan-owned balance or mailbox-like storage.

### Progression Perks / Listeners

There is visible progression/perk behavior inside the module.

- Treat it as clan-attached advancement behavior.
- Do not let it crowd out the main clan loop in the module context.
- It should be framed as opportunistic reward/enhancement behavior, not as the thing that defines what a clan is.

### Transport

Transport behavior appears to provide clan-aware navigation affordances.

- This is an attached player convenience/system-integration layer.
- It should be documented briefly rather than treated as the module's main story.

### Logging / Leaderboards / Tips / Settings / World Listeners

These systems are important support surfaces:

- observability and logs
- player-facing tips
- settings menus
- leaderboard views
- world listeners that enforce clan rules

They should be discoverable from this file, but they should remain secondary to membership, territory, resources, and conflict.

## Invariants And Easy-To-Break Rules

- A clan is a long-lived group-owned entity, not a temporary party.
- Membership rank is clan-local authority and should not be confused with global player rank.
- Clan state may remain meaningful even when no members are online.
- Clan lookup for online players often depends on metadata being correct.
- Offline clan lookup may require searching loaded clan objects by member UUID.
- Territory is chunk-based and should be treated as the authoritative world ownership model for the module.
- Admin clans and safe clans are real domain categories and can change relation and territory behavior.
- Relationship state is not cosmetic; it affects real gameplay and map/world interpretation.
- Pillage is not just a flag; it changes what players and clans are allowed to do.
- Many central clan rules are config-driven:
  - claims
  - member limits
  - pillage
  - dominance
  - bank/interest
  - energy
  - TNT and protection behavior
- Clan property persistence is repository-backed and may be flushed separately from gameplay-time mutations.
- Insurance is automated and not player-controlled.
- Post-pillage protection is a grace-period system, not a resource clans directly steer.
