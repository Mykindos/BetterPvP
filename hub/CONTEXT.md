# Hub Module Context

## Purpose

`hub` is the network lobby-and-routing runtime for the BetterPvP hub server.

It owns the shared hub-world experience that players land in before routing into other servers or lingering in hub-local activities. In practice, that means restoring and loading the canonical hub world, normalizing players into a safe lobby state, presenting server-selection surfaces, showing queue feedback, and enforcing hub-specific movement, interaction, and combat rules.

`hub` is not the owner of shared player identity, queue truth, or cross-server admission policy. It is the local runtime that makes those systems usable from the hub server.

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [core/CONTEXT.md](../core/CONTEXT.md)
- [champions/CONTEXT.md](../champions/CONTEXT.md)
- [orchestration/CONTEXT.md](../orchestration/CONTEXT.md)
- [private/store/CONTEXT.md](../private/store/CONTEXT.md)

`core` owns shared concepts such as `Client`, `Gamer`, ranks, action bars, sidebars, boss bars, combat infrastructure, item/menu frameworks, scene primitives, and generic world abstractions.

`hub` uses those shared systems to create a safe, navigable lobby runtime on one specific server. It does not redefine them.

## Ubiquitous Language

### `HubWorld`

The canonical main world used by the hub server. It is restored from `worlds/world.zip`, loaded as the main world, and treated as the authoritative runtime surface for hub behavior.

### hub spawn / `spawnpoint`

The required `PerspectiveRegion` used as the authoritative spawn location for hub joins, fallback teleports, and ordinary respawn behavior outside FFA overrides.

### hub bounds / `min_bounds` / `max_bounds`

The required `PointRegion` pair that defines the playable hub bounding box. Non-admin, non-creative players who move outside it are snapped back to hub spawn.

### `Zone`

The hub-local location classification used to drive behavior rules. Current zone values are `COMMON`, `FFA`, and `NONE`.

### `COMMON`

The ordinary hub area inside the hub world but outside the FFA arena. It is safe, heavily interaction-restricted, and grants hub-specific movement behavior like double jump.

### `FFA`

The local free-for-all arena inside hub. Entering it enables combat-facing behavior, swaps the player to an FFA loadout, and applies stricter combat-leave rules.

### `HubInventoryService`

The service that applies the player's current hub-facing inventory state. It owns the distinction between the simple hub hotbar and the FFA loadout.

### server selector

The menu or NPC-driven surface players use to browse network targets and request travel into clans, champions, or hub server instances.

### `HubQueueStatusRegistry`

The local runtime cache of queue status updates for players currently online on this hub server. It is a hub-side display cache, not the source of truth for queue state.

### queue status update

A queue snapshot for one player, coming from orchestration, describing target server, queue position, queue size, and related state used for action bar and command feedback.

### `OrchestrationGateway`

The HTTP integration used by hub to poll queue state and perform queue-management operations that are owned by the orchestration layer.

### hub scene / hub NPC

The set of scene-loaded NPCs, displays, and selector objects placed into the hub world through Mapper- and ModelEngine-gated scene loading.

### `InstanceSelectorNPC`

A hub NPC that represents a selectable server type and opens the relevant server-selection menu for players.

### trainer NPC

The hub NPC that routes players toward the local FFA surface and related training flow.

### store chest NPC

The hub NPC that exposes store-related entry points. It is a hub surface over store behavior, not the owner of store entitlement truth.

### hub hotbar

The simplified lobby inventory state applied to players in ordinary hub use. Currently its main interaction item is the server-select compass.

### FFA loadout

The combat-ready inventory state applied when a player enters the hub FFA arena. It is built from champions role weapon logic plus basic consumables.

## Primary Hub Lifecycle And Main Flows

### 1. Boot And Runtime Setup

Hub starts by restoring the canonical main world before normal plugin enable logic runs. `HubBootstrap` resolves the server `worlds` directory, deletes any existing main-world folder, and extracts `worlds/world.zip` into place.

Once the plugin enables, `Hub` creates its injector, wires config-backed fields, registers listeners and commands, loads updater tasks, and initializes adapter-gated integrations. At this point the module is ready to act as the runtime owner for lobby behavior.

`HubWorld` then acts as the authoritative model of the loaded hub world. It reads Mapper-exported region data and metadata from the restored world, requires key regions such as `spawnpoint`, `min_bounds`, and `max_bounds`, computes the hub bounds, and applies world rules suitable for a static lobby surface.

Scene content is layered on afterward. `HubSceneLoader` uses Mapper regions plus ModelEngine-backed scene loading to spawn selector NPCs, store/trainer NPCs, floating displays, and role kit selectors when its required integrations are available.

### 2. Player Arrival And Normalization

When a player arrives in hub, the module first normalizes them into a safe lobby state rather than preserving arbitrary prior runtime state.

Combat features are disabled. Join and quit broadcasts are suppressed. The player is set to adventure mode, healed to max health, stripped of xp progress, and teleported to the hub spawnpoint. Hub then applies the ordinary hub hotbar and initializes zone state from the player's location.

This normalization matters because hub is not just a passive spawn area. It is a controlled staging runtime where routing, queue display, and world restrictions assume a clean baseline.

### 3. Routing And Queue Flow

From that normalized state, the main player loop is navigation and routing.

Players use the server-select hotbar item or hub selector NPCs to open menus that represent available server types and concrete targets. Those menus surface live player counts and let players request travel into available hub, clans, or champions instances.

Travel initiation itself is split across integrations:

- hub UI sends queue or join requests outward through plugin messaging
- orchestration-backed APIs are used for queue inspection and queue-management operations

While a player remains in hub, the module polls orchestration for that player's queue status. The resulting updates are filtered to the current server, cached in `HubQueueStatusRegistry`, and surfaced locally through:

- action bar queue status
- periodic chat reminders
- `/queue` self-service commands
- admin `/queue` management commands

Hub therefore owns the local queue user experience, but not the queue's canonical state machine.

### 4. Local Hub Movement And Runtime Rules

The rest of the module is about keeping players inside a predictable world with different behavior in different hub-local zones.

Zone resolution is location-based:

- `FFA` if the player is inside the FFA arena region
- `COMMON` if the player is elsewhere in the hub world
- `NONE` if the player is outside the hub world entirely

The common hub area is safe and heavily restricted. Non-creative players cannot freely place or break blocks there, most common-area interactions are suppressed, and double jump is granted specifically while in common hub space.

Hub also enforces the hub bounding box. If a non-admin, non-creative, non-spectator player moves outside the playable hub bounds, they are sent back to spawn.

Entering the FFA zone changes the rules substantially. Combat becomes active, role placeholder and armor presentation change, the player receives an FFA loadout, and hub-safe movement/interaction assumptions no longer apply in the same way. Leaving FFA returns the player to the hub hotbar and safe-lobby presentation state.

## State Model And Ownership

`core` owns:

- shared `Client` and `Gamer` identity
- ranks and rank formatting
- combat truth and combat timers
- action bar, boss bar, sidebar, and title frameworks
- generic item, menu, and inventory infrastructure
- scene primitives and world abstractions
- shared plugin messaging channels

`orchestration` owns:

- queue truth
- queue admission state
- cross-server routing policy
- server-target queue snapshots and status decisions

`champions` owns:

- role identity
- role weapon/loadout truth
- combat-role presentation elements reused by hub FFA and kit selectors

`private/store` owns:

- store entitlement truth
- store fulfillment behavior
- store product semantics behind store-linked hub surfaces

`hub` owns:

- hub-world runtime rules
- hub-local zoning and zone transitions
- join normalization into lobby-safe state
- hub inventory state switching
- selector menus and selector NPC surfaces
- local queue-status cache and queue-facing UX
- local FFA pocket behavior

Important ownership notes:

- queue status inside hub is cached runtime state, not canonical persisted queue truth
- most hub state is ephemeral and session-scoped
- the important durable surfaces in this module are file-backed world and Mapper scene data, not repository-backed domain records
- player identity is shared-core-owned even when hub is temporarily controlling a player's location, inventory, combat availability, and presentation

## Integration Points

### `core`

Hub is deeply built on `core` player, combat, UI, item, world, and scene abstractions. A contributor changing those shared systems can easily affect join normalization, queue display, NPC interaction, or FFA behavior.

### `orchestration`

Hub relies on orchestration for queue status reads and queue-management operations. It also relies on the broader network routing contract for server selection to result in actual travel.

### `champions`

Hub's local FFA is not a standalone combat system. It reuses champions role concepts and weapon-equipping behavior, and the scene layer also spawns kit selectors based on champions roles.

### `private/store`

Hub exposes a store chest NPC as a routing surface into store-related behavior, but store products and entitlement state remain owned elsewhere.

### scene integrations

Hub scene loading depends on integrations like Mapper and ModelEngine. Without those adapters and expected models, hub still has its runtime rules, but key NPC and presentation surfaces may not load.

## Attached Subsystems

### Local FFA Arena

The hub FFA arena is the largest attached subsystem.

It uses a named `FFA` polygon region plus an `ffa_spawnpoint` to create a self-contained combat pocket inside the hub server. Entering FFA swaps the player from hub hotbar to FFA loadout, enables combat-facing features, and changes visible combat presentation.

FFA-specific behavior includes:

- zone-driven enter and exit handling
- FFA loadout application
- FFA respawn handling at the arena spawnpoint
- combat-lock prevention when attempting to leave the arena
- extra damage, velocity, and fire checks to stop cross-boundary combat leakage
- temporary red-glass wall rendering around arena edges for combatants

This subsystem is important, but it is still attached to the broader hub runtime rather than the primary identity of the module.

### Scene And Authoring Surface

Hub depends heavily on named Mapper data points and integration-gated scene loading.

Important named regions include the required spawn and bounds points, the FFA region, FFA display points, server-selector NPC points, and role kit-selector points. These are documented further in [MAPPER_REGIONS.md](./MAPPER_REGIONS.md).

`HubSceneLoader` builds scene content such as:

- store chest NPC
- trainer NPC
- instance selector NPCs
- coming-soon NPCs
- floating text displays
- role kit selectors

This surface shapes how the hub feels and how players discover actions, but it exists to support the lobby-routing runtime rather than define it.

### Hub Presentation

Hub also owns several presentation layers that make lobby state legible:

- a sidebar that currently focuses on rank and network online count
- player-list display names and nametags with rank formatting
- a combat timer boss bar that only appears when combat features are active and the player is actually in combat

These are secondary to the routing/runtime flow, but they are still important contributor touchpoints.

### Admin And Ops Surfaces

Operational controls are intentionally small but important:

- `/hub reload` reloads the module's runtime-facing content
- `/queue` exposes player queue status
- admin queue commands expose queue snapshot viewing and operational actions such as pause, resume, admit, and remove

These commands are not the core player experience, but they matter for live server operations.

## Invariants And Easy-To-Break Rules

- The hub world is expected to be restored from `world.zip` before normal runtime use. Breaking bootstrap restore logic risks subtle world drift between restarts.
- `HubWorld` requires `spawnpoint`, `min_bounds`, and `max_bounds`, and practical hub runtime also expects FFA and selector-related Mapper points to exist.
- Zone resolution is intentionally simple and location-based. If contributors add more nuanced location logic, they should preserve the current `FFA` vs `COMMON` vs `NONE` mental model unless they intentionally redesign it.
- Interaction and inventory restrictions are much stricter in `COMMON` hub space than in `FFA`. Casual refactors can easily weaken that separation.
- Queue truth does not live in `hub`. It is polled from orchestration and cached only for online players on this server.
- Queue status updates are filtered by `currentServer`. A contributor removing that filter could leak irrelevant queue state into the wrong hub server.
- Server join requests are initiated from hub UI through plugin messaging, while richer queue reads and admin operations use `OrchestrationGateway`. Both paths matter.
- Combat features are disabled on join and only selectively re-enabled on FFA entry. Missing an enter/exit edge case can leave players combat-enabled in safe hub space or combat-disabled in FFA.
- FFA exit during combat is blocked for non-admin players, including movement and teleport edge cases. That rule is reinforced in multiple places for a reason.
- Scene loading depends on adapters and available models. Hub NPC surfaces are not guaranteed unless the required integrations are present.
- No central hub-owned repository-backed persistence appears to define module identity. Contributors should think file-backed world data first, not database-first.
