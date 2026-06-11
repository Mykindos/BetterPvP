# Progression Module Context

## Purpose

`progression` is the professions-first player advancement module.

Its job is to model how players advance through profession activities such as `mining`, `woodcutting`, and `fishing`, how that advancement turns into levels and skill points, and how profession builds then feed back into live gameplay through attributes, items, loot, and listeners.

This is not a generic account-wide achievement layer. In the current code shape, Progression is primarily a structured gathering and profession-growth system with buildable skill trees.

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [core/CONTEXT.md](../core/CONTEXT.md)

`core` still owns the shared player model and the generic infrastructure Progression builds on:

- shared player identity such as `Client` and `Gamer`
- generic persistence and repository patterns
- shared property/event infrastructure
- shared item loading and item access infrastructure
- settings/menu frameworks
- shared reward and loot infrastructure

`progression` should not redefine those concepts. It should explain how profession systems use them.

## Ubiquitous Language

### `Profession`

A named progression track representing a profession-specific gameplay loop. Current canonical professions in code are `mining`, `woodcutting`, and `fishing`.

### `ProfessionHandler`

The profession-specific coordinator for one profession. A `ProfessionHandler` owns profession config loading, skill-point rules, skill tree loading, and access to that profession's player data.

### `ProfessionProfile`

The player-owned top-level progression container for one player. It is keyed by the player's UUID and holds a map of profession names to `ProfessionData`.

### `ProfessionData`

The player-owned state for one profession. It holds the current profession experience, the derived level, the profession build, and any profession-specific properties.

### profession experience / level

The numeric advancement state for a profession. Experience is the persisted source of truth; level is derived from the profession experience curve.

### skill points

The spendable progression currency earned from profession level progression. Available skill points are derived from the current profession level and that profession's `skillPointInterval`, minus the levels already invested into build nodes.

### `ProfessionBuild`

The persisted allocation of unlocked node levels for one profession. It stores which `ProfessionNode`s the player has invested in and at what level.

### `ProfessionNode`

A node in a profession skill tree. Nodes can represent profession skills, profession attributes, recipe unlocks, or other profession-specific build investments.

### profession skill / attribute / recipe node

Different node shapes layered onto the same build model:

- profession skill nodes unlock or level active profession capabilities
- profession attribute nodes modify numbers or rules in the profession loop
- recipe nodes unlock profession-related crafting or recipe access

### skill tree layout

The authored node graph for a profession. In the current code, tree layouts are loaded from profession-local files and prefer `skill_tree.drawio`, then `skill_tree.xml`, then `skill_tree.yml`.

### profession properties

Profession-specific persisted key/value state attached to `ProfessionData` via the shared property container model. These properties are important because some gameplay systems react to property changes, not just to raw XP or level changes.

### booster

A time-limited modifier that increases profession XP gain for a player. Boosters are player-owned progression modifiers, not profession definitions.

### profession-specific activity records

Supporting progression records that track profession outputs beyond XP alone, such as fishing catches, mining totals, and woodcutting logs. These feed reporting, leaderboards, and profession-specific behaviors.

## Primary Progression Lifecycle And Main Flows

Progression is easiest to understand as a repeating profession loop:

1. A player's `ProfessionProfile` is loaded through the profile manager and repository flow.
2. Profession-specific `ProfessionData` is created lazily when a profession entry is needed and does not already exist on the loaded profile.
3. The player performs profession activities such as mining ores, chopping logs, or catching fish.
4. Gameplay grants profession XP through profession flows that surface as `PlayerProgressionExperienceEvent`.
5. Profession level is derived from the accumulated experience curve rather than stored separately.
6. Rising level increases available skill points according to the profession's `skillPointInterval`.
7. The player spends skill points into profession nodes on that profession's skill tree.
8. The resulting `ProfessionBuild`, profession properties, listeners, and profession items change what the player can do next in the live profession loop.
9. Progression writes are persisted through a mix of queued stat/property flushes and separate build persistence updates.
10. The system surfaces current progression through commands, menus, tips, and leaderboards.

This means the main gameplay identity is not just "gain XP." It is:

- perform profession activity
- grow profession XP and level
- invest into the build
- unlock stronger or broader profession behavior
- repeat the profession loop with a changed toolset

### Profile load

The main player-facing progression state begins with `ProfessionProfile`. The profile is loaded for a player UUID and populated from repository-backed persistence. The profile itself is a profession container, not the per-profession state.

### Lazy profession data creation

`ProfessionData` is commonly created on demand when a loaded profile exists but has not yet touched a specific profession entry. This is an important contributor assumption because not every player will already have persisted rows for every profession.

### Experience and level flow

Experience is the canonical persisted advancement value. Level is recalculated from the profession experience curve when needed. Contributors changing XP gain, XP modifiers, or level-based skill-point rules are changing the main advancement economy of the module.

### Skill-point and build flow

Build progression is tied directly to level progression. Available skill points are computed from level and `skillPointInterval`, then reduced by already-invested node levels. Players are not just earning levels; they are converting those levels into node investments that define their current profession build.

### Runtime application flow

A profession build becomes meaningful only when its nodes, properties, and listeners alter live gameplay. This is where profession attributes, unlocked items, loot behavior, and environment listeners connect the persisted build back into real player actions.

### Persistence flow

Progression persistence is not a single monolithic save. The code currently treats:

- experience updates as hot-path queued upserts
- property updates as separately queued writes
- build updates as their own persistence flow

That split matters because gameplay mutation happens more often than database flushes, and different parts of progression state are saved through different paths.

## State Model And Ownership

`progression` owns player progression state, but it does not own player identity itself.

### What `core` owns

`core` owns:

- shared player identity such as `Client` and `Gamer`
- shared realm/season context
- generic event and property infrastructure
- item loading, item access, and shared item abstractions
- generic repository and persistence patterns
- menu/settings frameworks used to surface progression

### What `progression` owns

`progression` owns:

- profession definitions
- profession config loading
- profession XP and derived levels
- skill-point rules
- profession builds and build-node persistence
- profession properties
- profession-specific activity tracking
- progression boosters
- profession-facing skill tree rules and layout loading

### Player-owned, not clan-owned

Progression state is player-owned. Other modules can react to it or modify how profession rewards are calculated, but they do not own progression truth. Even where Clans integrates with Progression through adapters and listeners, the authoritative progression state still lives on the player's profile and profession data.

### Season-scoped persistence

Current persistence is keyed by client plus season plus profession for major progression tables. Contributors should think of the current progression model as player-owned but season-scoped by the current realm season. The player identity is shared across the project, but the profession progression data in this module is not simply global account state.

### Player state and profession state are linked, not interchangeable

A player can exist without fully populated profession data for every profession. A shared `Client` or `Gamer` is not the same thing as a fully loaded `ProfessionProfile`, and a `ProfessionProfile` is not the same thing as a single `ProfessionData` entry.

## Integration Points

Progression is tightly integrated with shared infrastructure and lightly integrated with several gameplay modules.

### `core`

`core` provides the most important contracts:

- `Client` lookup and identity
- profession XP events such as `PlayerProgressionExperienceEvent`
- shared property containers and property parsing
- shared item loading and access services
- generic menu/settings infrastructure
- loot entry registration and other shared plugin bootstrapping

### profession adapters and external listeners

The module loads plugin adapters and can also be targeted by `@PluginAdapter("Progression")` in other modules. This is the main cross-module extension point contributors should expect when changing progression behavior.

### `clans`

Clans currently integrates with Progression through adapters and progression listeners. That integration can modify profession rewards or behavior in clan-controlled spaces, but it does not change the ownership model: progression remains player-owned rather than clan-owned.

### events and other gameplay modifiers

Other modules can react to progression XP events or progression-owned hooks to add multipliers, alter rewards, or change profession behavior in special circumstances. These are integrations around the main profession loop, not replacements for it.

## Attached Subsystems

These systems matter, but they are attached to the main profession loop rather than defining the module's primary identity.

### Skill trees and builds

This is the most important attached subsystem after leveling.

Progression does not stop at XP. Profession skill trees define the authored build space players invest into, and `ProfessionBuild` persistence records the chosen node levels. Tree layouts are loaded from authored files, with a preference order of `drawio`, then `xml`, then `yml`. Contributors working on profession depth will often be changing this layer as much as the raw XP loop.

### Custom loot and profession items

Progression adds a large profession-specific item and loot surface around the profession loop, including fish, bait, profession tools, unlockable profession items, and interaction items. These are important because they make build choices visible in live play, but they are layered onto profession identity rather than replacing it.

### Bait, fish, tools, and interaction items

Fishing in particular carries a deep attached surface of bait, fish types, fishing rewards, and item-driven abilities. Mining and woodcutting also expose profession-specific tools, drops, and item interactions.

### Boosters

Boosters temporarily increase profession XP gain and act as a player-owned modifier over the normal advancement loop. They are important to progression pacing, but they are not the main profession identity.

### Leaderboards

Leaderboards consume profession activity and progression outputs such as fishing totals, biggest catches, mined materials, or logs chopped. They expose progression performance without becoming the source of truth for profession state.

### Settings menus

Settings and menu surfaces let players view or navigate profession-related options. They present progression state; they do not define the progression model.

### Tips

Progression tips are onboarding and nudging surfaces for profession behavior. They help players understand the loop but are not part of the core state model.

### Commands and admin tools

Commands such as progression stats and wipe flows are administrative or visibility tools wrapped around the main profession system.

### Profession-specific listeners and world behaviors

A large number of listeners implement the live consequences of profession nodes, profession properties, and profession items. These behaviors are important to gameplay feel, but they should usually be understood as runtime effects of the main profession/build model.

## Invariants And Easy-To-Break Rules

- Treat Progression as a professions module first, not a generic achievement system.
- Do not redefine `Client`, `Gamer`, `Realm`, or shared item infrastructure inside this module doc.
- Experience is the persisted source of truth for profession advancement; level is derived from the experience curve.
- Available skill points are tied to profession level and `skillPointInterval`, then reduced by invested node levels.
- Profession data may be absent until lazily created for a loaded profile; code and docs should not assume every profession entry already exists.
- Skill tree source resolution matters: `skill_tree.drawio` wins over `xml`, which wins over `yml`.
- Build persistence is not the same write path as queued XP/property flushes.
- XP writes are hot-path queued upserts; property writes are separately queued and flushed in batch.
- Progression persistence is currently season-scoped, even if older naming suggests "global" behavior.
- Some progression gameplay effects depend on profession property change events, not just on raw XP totals or derived level.
- Cross-module adapters can modify progression behavior, but they do not become the owner of progression truth.
