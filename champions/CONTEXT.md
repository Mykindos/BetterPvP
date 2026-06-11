# Champions Module Context

## Purpose

`champions` is the class / role-based PvP combat system for BetterPvP.

Its primary job is to define how players fight through:

- role selection
- build composition
- skill execution
- energy and cooldown constraints
- role-specific combat stats and behavior

This module is not just a pile of combat abilities. It gives the project a structured combat identity built around named roles, configurable skills, and persisted builds that players bring into live PvP.

At a high level, Champions owns:

- role/class identity for combat
- persisted player builds
- role-specific and global skills
- combat-facing energy and cooldown usage
- Champions-specific combat stats and role stats
- attached custom items and item abilities that layer onto the role/skill loop

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [core/CONTEXT.md](../core/CONTEXT.md)

This module builds on shared concepts from `core`:

- `Client` remains the shared player identity.
- `Gamer` remains the player's realm-scoped gameplay state.
- generic combat infrastructure remains in `core`.
- generic cooldown, effects, throwable, and energy services remain in `core`.
- shared stats and persistence patterns remain in `core`.

This file does not redefine those concepts. It describes how Champions composes them into a class-based combat system.

## Ubiquitous Language

### Role

A `Role` is the primary combat class a player equips inside Champions.

- Roles define baseline combat identity.
- Roles influence health, armor/material identity, and the set of skills available for build composition.
- The current visible role set is:
  - `ASSASSIN`
  - `KNIGHT`
  - `BRUTE`
  - `RANGER`
  - `MAGE`
  - `WARLOCK`
- `KNIGHT` is the default role in the current code shape.

### Role Selection

Role selection is the act of equipping a role onto a living entity, usually a player.

- Role changes are evented.
- A player's current role is persisted as a Champions property.
- Role changes affect health and combat presentation.

### Build

A build is a persisted role-specific skill loadout for a player.

- Builds belong to a player through `GamerBuilds`.
- A build is tied to a specific role.
- A role can have multiple builds by ID.
- Builds are not just temporary UI state; they are stored and reloaded.

### RoleBuild

A `RoleBuild` is a single configured build for one role.

- It contains the chosen skills for that role.
- It distributes build points across eligible skills.
- It is the unit players edit, save, apply, and randomize.

### GamerBuilds

`GamerBuilds` is the collection of a player's Champions builds.

- It is attached to a shared `Client`.
- It owns the player's stored builds.
- It also tracks active builds by role.

### Skill

A `Skill` is the main executable combat behavior unit in Champions.

- Skills are configurable.
- Skills belong either to a role or to the global skill set.
- Skills can consume energy, incur cooldowns, apply effects, move players, deal damage, control space, or alter combat state.
- Skills are the main reason role choice becomes real gameplay rather than only flavor.

### Skill Type

Skills carry gameplay tags or categories such as:

- cooldown-based
- energy-based
- movement
- damage
- buff
- debuff
- defensive
- offensive
- utility
- team
- world

These types matter because they affect how skills are understood, described, and constrained.

### Passive Skill

A passive skill changes combat behavior without being a straightforward one-shot activation.

- Passive does not mean unimportant.
- Many passives still hook deeply into damage, energy, and combat-event flows.

### Active / Toggle / Channel Skill

Many skills are player-driven activated behaviors.

- some spend energy directly
- some use cooldowns
- some toggle on/off
- some channel or drain over time

These should be understood as variations of the same core skill-execution model rather than separate feature families.

### Energy

Energy is a combat resource used by many Champions skills.

- Champions does not own the generic energy service, but it depends on it heavily.
- Energy cost is one of the main balancing and pacing constraints on active combat behavior.
- Energy interaction is part of the main Champions combat loop, not a side system.

### Cooldown

Cooldown is another primary combat constraint.

- Many skills are cooldown-gated.
- Cooldown management is part of how the module keeps roles distinct and abilities readable.

### Combat Role Identity

Each role has a recognizable combat identity:

- Assassin: agile, punishing, opportunistic
- Knight: aggressive, durable front-line pressure
- Brute: crowd control and durability
- Ranger: ranged bow pressure
- Mage: elemental/support/control identity
- Warlock: health-cost and health-pressure identity

This identity is important context for contributors making combat changes. A skill is rarely just "a mechanic"; it should fit the role's combat job.

## Primary Combat Lifecycle And Main Flows

This is the main loop contributors should have in mind when changing the module.

### 1. Module Boot And Content Registration

The module starts by loading Champions content and runtime surfaces.

Current visible boot responsibilities include:

- database migrations
- listener registration
- command loading
- skill loading
- tip loading
- leaderboard registration
- achievement loading
- adapter loading
- item loading and serializer registration

The important conceptual point is that Champions is content-driven: roles, skills, builds, and items are loaded into a shared runtime rather than hardcoded into one simple loop.

### 2. Role Selection Lifecycle

The first player-facing Champions decision is role choice.

Core transitions include:

- populate player's current role from persisted property
- equip a role
- change role
- apply role-dependent health and item behavior
- persist the selected role for future sessions

Important shape:

- role selection is not only cosmetic
- role changes update live combat state
- role equip events are meaningful integration points

### 3. Build Lifecycle

Players do not just choose a role; they choose how that role is configured.

Core transitions include:

- load player builds
- load default builds
- create or edit build
- equip skills into a build
- de-equip skills
- apply build
- delete build
- generate random build

Important shape:

- builds are persisted player-owned combat configuration
- builds turn a role into a customized combat package
- the build system is one of the module's core identities, not just menu infrastructure

### 4. Skill Selection And Composition

Within a role, players compose skill choices.

Core shape:

- a role exposes eligible skills
- skills are chosen and leveled within build-point constraints
- different skill types shape what the build can do
- one build expresses a combat playstyle for one role

This is where the role system becomes flexible rather than static.

### 5. Skill Execution In Combat

Once a role and build are active, live combat is mostly about skill execution under constraints.

Core shape:

- player uses a skill
- the module checks relevant ability constraints
- energy and/or cooldown rules apply
- skill behavior modifies combat, movement, effects, projectiles, or world interaction
- combat results feed back into shared damage/effect/stat systems

Important shape:

- skills are the main executable unit of Champions combat
- energy and cooldowns are the main pacing constraints
- many abilities interact with shared combat events rather than operating in isolation

### 6. Ongoing Combat State

Champions is not only about button-press abilities.

The module also maintains longer-lived combat behavior through:

- passive skills
- role effects
- damage modifiers
- energy interactions
- effect application
- throwable/projectile behavior
- role-specific stat tracking

Contributors should treat these as part of the same combat system, not as unrelated helper code.

### 7. Combat Outcome And Statistics

Champions tracks combat outcomes in role-aware ways.

Current visible outcomes include:

- role statistics
- combat data
- kill-related tracking
- leaderboards by class/role and combat dimensions

These are downstream of the main role/skill combat loop, but they are still first-class module behavior.

## State Model And Ownership

### What `core` Owns

`core` owns shared player and combat infrastructure:

- `Client`
- `Gamer`
- shared combat event/model infrastructure
- generic energy service
- generic cooldown manager
- effect manager
- throwable infrastructure
- shared persistence patterns
- shared stat infrastructure

### What `champions` Owns

`champions` owns the class-based combat layer:

- role identity
- role selection and persistence of current role
- player build configuration
- role- and global-skill registration
- skill configuration and execution semantics
- role-specific combat stats and leaderboards
- Champions-specific settings and combat-facing UX

### Important Ownership Boundaries

- A player's shared identity is still owned by `core`; Champions adds a combat specialization layer to that player.
- Role selection is Champions-owned state, but it is stored through shared player property mechanisms.
- Energy is not a Champions-owned generic resource system, but Champions is one of its major consumers.
- Generic combat plumbing lives in `core`; role/build/skill meaning lives in Champions.

### Persistence Shape

Important current persistence patterns include:

- current role is persisted through Champions properties on the player path
- builds are repository-backed and reloaded for players
- combat statistics are stored in Champions-specific stats repositories layered on shared combat stats infrastructure

## Integration Points

### Integration With `core`

Champions depends heavily on `core` for:

- player lookup through `ClientManager`
- combat events and damage systems
- energy service
- cooldown management
- effect management
- throwable handling
- shared stats and achievements infrastructure
- item factory and shared item systems

### Integration With Live Combat

Champions is deeply combat-facing.

It integrates with:

- damage events
- effect application
- energy use and degeneration
- projectile and throwable behavior
- role-driven health changes
- death and kill tracking

### Integration With Shared Player State

Champions stores some player-facing state through shared property systems.

Visible current examples:

- current role
- Champions settings like skill preview / tooltip preferences

### Integration With Content And Adapters

The module is adapter-aware and content-heavy.

Important current shape:

- plugin adapters are loaded at boot
- items are loaded through shared item infrastructure
- skills are loaded as module content

This means contributors should think in terms of registered content plus runtime systems, not just direct code paths.

## Attached Subsystems

These are current and important, but they are attached to the main role/build/skill combat loop rather than defining the primary identity of the module.

### Custom Items And Item Abilities

Champions adds a very large custom item surface.

- Core handles the shared item framework.
- Champions contributes a large amount of concrete custom items and item-driven abilities.
- These items should be described as a major attached subsystem layered onto the main Champions combat loop.

Important framing:

- items are significant
- items affect real gameplay
- but the module's primary identity is still role-based combat rather than "all custom items"

### Menus, Selectors, And NPC Affordances

The module includes a large set of player-facing selection surfaces:

- class selection
- build editing
- skill selection
- kit menus
- selector items
- NPC-based access points

These are important delivery mechanisms for the core role/build system.

### Stats, Leaderboards, And Achievements

Champions tracks and presents role-aware combat performance.

- role statistics
- combat leaderboards
- achievements linked to skill or role usage

These are important support surfaces, but they are downstream of the main combat loop.

### Settings And UX Helpers

Champions includes player-facing quality-of-life settings and tips.

Examples visible now:

- skill chat preview
- weapon skill tooltip settings
- settings menu
- Champions tips

These should be documented as attached UX surfaces rather than core combat identity.

### Role Presentation / Packet / Armor Systems

There is visible packet/remapping and role-presentation behavior tied to armor and placeholders.

- These systems help make roles visible and coherent in live play.
- They support the role system rather than replacing it.

## Invariants And Easy-To-Break Rules

- Champions is a class/role combat module first, not a generic item module.
- Role changes are meaningful combat-state changes and should not be treated as cosmetic only.
- Builds are persisted combat configuration, not disposable menu state.
- Skills are configured content with runtime constraints; changing one skill can alter role identity, balance, and player build composition.
- Energy and cooldowns are central pacing constraints for the module.
- Generic combat infrastructure may live in `core`, but role/build/skill meaning belongs in Champions.
- Custom items and item abilities are large and important, but they should stay conceptually attached to the role/skill combat loop.
- Player property changes like current role or Champions settings are stored through shared player state, so changes here can ripple into shared lifecycle behavior.
- Role-specific combat stats and leaderboards should remain aligned with role equip/build/skill behavior.
