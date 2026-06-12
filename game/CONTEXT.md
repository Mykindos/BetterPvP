# Game Module Context

## Purpose

`game` is the team-match runtime for Champions-style PvP modes.

Its job is to run a single server's rotating match loop:

- choose a game
- choose a compatible map
- move the server through `WAITING -> STARTING -> IN_GAME -> ENDING -> WAITING`
- track participants, spectators, and teams during that loop
- persist enough match-history context for stats and reporting

This module is not a generic gameplay bucket and it is not the owner of raw combat truth. It is the orchestration layer that turns shared player state, Champions combat kits, maps, selectors, and game-specific controllers into a running match server.

At a high level, `game` owns:

- server-wide match orchestration
- current game and current map rotation
- match-state transitions
- participant and spectator runtime state
- team runtime and balance rules
- match-scoped attributes
- game-local stat context through `GameInfo`
- current concrete modes such as `CaptureTheFlag` and `Domination`

## Relationship To Shared Context

Read these first:

- [../CONTEXT.md](../CONTEXT.md)
- [../core/CONTEXT.md](../core/CONTEXT.md)
- [../champions/CONTEXT.md](../champions/CONTEXT.md)

`core` still owns the shared concepts that `game` builds on:

- shared player identity such as `Client` and `Gamer`
- shared combat, damage, effects, and generic stat infrastructure
- generic persistence and property patterns
- shared chat, sidebar, action-bar, title, and utility helpers
- generic plugin bootstrapping, event, and adapter infrastructure

`champions` still owns:

- role identity
- builds
- skills
- combat-kit truth

This file should not redefine those concepts. It should explain how `game` uses them to run a rotating match server.

## Ubiquitous Language

### `AbstractGame`

The base runtime concept for a match type registered on the server.

- It owns a game configuration.
- It can set up and tear down match-local behavior.
- It decides how winners are determined.
- It exposes a participant model and winner description.

### `TeamGame`

The main current shape of `AbstractGame`.

- It is a game with explicit `Team`s.
- It owns team membership during the match lifecycle.
- It provides shared team-game logic such as assignment, removal, reset, balance checks, and default graceful-ending behavior.

### `GameRegistry`

The registry that knows which games exist on this server and which `GameModule` belongs to each one.

- It currently registers `CaptureTheFlag` and `Domination`.
- It is also responsible for entering and exiting game-scoped injectors and listeners when a match becomes active.

### `GameModule`

The Guice module and scoped runtime bundle for one concrete game.

- A `GameModule` holds game-specific bindings and listener classes.
- Its lifecycle is tied to the active match, not the whole server.

### `ServerController`

The top-level owner of current server match state.

- It owns the `GameStateMachine`.
- It owns the current selected game.
- It owns whether the server is accepting players.
- It switches between lobby `GameInfo` and live-game `GameInfo`.

### `GameStateMachine`

The server's match-state transition controller.

- It starts in `WAITING`.
- It exposes guarded transitions and enter/exit hooks.
- Most runtime behavior in this module hangs off state transitions.

### `GameState`

The canonical phases of the match server:

- `WAITING`
- `STARTING`
- `IN_GAME`
- `ENDING`

These are strict lifecycle states, not just UI labels.

### `MappedWorld`

The module's map abstraction around a zipped world template plus metadata and regions.

- A `MappedWorld` can represent the waiting lobby or a match map.
- Maps are matched to games using metadata game mode.

### waiting lobby

The special shared world used while the server is not actively running a match.

- It is required for startup.
- It hosts team selectors, kit selectors, and pre-match setup surfaces.

### current map

The active match map selected for the current game.

- It must match the selected game's configured mode/name expectations.
- It is separate from the waiting lobby.

### `Participant`

The module-local wrapper for an online player in the game runtime.

- It links a Bukkit `Player` to a shared `Client`.
- It tracks alive/dead state.
- It tracks spectating state.
- It tracks whether spectating should persist into the next game.

### spectator / persistent spectator

A spectator is a `Participant` who is not currently playing the live match.

- A normal spectator may only be spectating this match.
- A persistent spectator is marked to spectate the next game as well.

This distinction matters for balancing and lobby/match transitions.

### `GameInfo`

The module-owned stat context record for a lobby or a live match.

- It stores a generated game id.
- It stores game and map identity.
- It stores a snapshot of player-to-team assignment.

It is the runtime context that later gets written out for stat attribution.

### game attributes / bound attributes

The configurable knobs that shape one game's runtime behavior.

Current important examples include:

- required players
- max players
- respawns
- respawn timer
- game duration
- allow late joins
- team-balance rules
- mode-specific win-scoring rules

Bound attributes are game-local and are discarded when the active game changes.

### team selector

The waiting-lobby NPC surface players use to choose a team before a match.

### kit selector

The waiting-lobby or match-map NPC surface players use to choose a Champions role and build setup relevant to this server mode.

### powerup

A game-local world pickup or spawned runtime bonus managed by the `game` framework.

- Powerups are not the primary identity of the module.
- They are attached match mechanics layered onto concrete game modes.

## Primary Game Lifecycle And Main Flows

The easiest way to understand `game` is as one server repeatedly cycling through a controlled match loop.

### 1. Boot And Runtime Setup

At plugin enable, the module performs server-level setup:

1. enable the plugin
2. run game-specific migrations
3. register listeners, commands, achievements, and adapters
4. register the concrete games in `GameRegistry`
5. load the waiting lobby and scan zipped maps
6. select an initial game and compatible map
7. mark the server as accepting players

Important current behavior:

- `GamePlugin` sets current mode to `CHAMPIONS`
- the waiting lobby must exist or startup fails
- maps are loaded from zipped templates under the plugin data folder
- maps are filtered by metadata so only compatible maps are selectable for a given game

### 2. Lobby And Pre-Match Flow

While the server is in `WAITING`, the next match is already defined even though it is not live yet.

The current pre-match loop is:

1. a player joins
2. the module wraps them as a `Participant`
3. the current selected game exists in `WAITING`
4. the waiting lobby is loaded
5. team selectors and kit selectors reflect the current game
6. players choose team, spectator state, and optionally Champions role/build/hotbar setup
7. the module keeps checking whether `RequiredPlayersAttribute` and `StartPausedAttribute` allow the server to move into `STARTING`

Important shape:

- the current game can only change while the server is in `WAITING`
- the server cannot leave `WAITING` unless a current game exists
- `WAITING` is not empty idle time; it is the staging phase for the next match

### 3. Match Runtime Flow

Once enough players are present and start is not paused, the server moves into the live-match loop.

The current flow is:

1. `STARTING` begins the pre-match countdown
2. if player requirements are still satisfied, the server transitions into `IN_GAME`
3. `GameRegistry` activates the selected game's scoped injector and module
4. the current map is loaded and start announcements/countdown run
5. participants are spawned into the live match flow
6. inventories refresh and game-specific controllers take over
7. deaths, respawns, spectating, balancing, scoring, objectives, and powerups run until the game ends

Important current runtime responsibilities:

- `PlayerController` derives player capabilities from both `GameState` and participant state
- game-specific listeners only exist while the active game is active
- team membership lives inside the selected `TeamGame`
- match attributes shape countdowns, respawns, durations, late joins, and balance rules

### 4. Ending And Rotation Flow

When a winner is found or the game force-ends, the server enters `ENDING`.

The current ending loop is:

1. winners are chosen from game-specific rules
2. ending titles, sounds, and slow-motion presentation run
3. `GameInfo` and team assignments are saved for stats
4. inventories and match-local listeners are cleaned up
5. the server returns to `WAITING`
6. a new game and map are selected for the next cycle

Important shape:

- ending is a real state with its own presentation and cleanup logic
- the game-scoped injector/module is torn down after match exit
- lobby `GameInfo` and live-game `GameInfo` are different records and should stay documented that way

## State Model And Ownership

### What `core` Owns

`core` owns:

- shared `Client` and `Gamer`
- shared combat, damage, and effect infrastructure
- shared chat/display/runtime helpers
- generic stat/property persistence patterns
- generic event and plugin lifecycle plumbing

### What `champions` Owns

`champions` owns:

- role identity
- build data
- skill definitions and execution truth
- combat-kit meaning

### What `game` Owns

`game` owns:

- the server-wide match loop
- current game rotation
- current map rotation
- participant and spectator runtime state
- team runtime and balance behavior
- game-scoped attributes
- match-local winner/loser flow
- lobby and live-match `GameInfo` context
- hotbar layout persistence for this server mode

### Important Boundaries

- Player identity is shared-core-owned, not game-owned.
- Match state is game-owned, not core-owned.
- `Participant` is a runtime wrapper around a shared player model, not a replacement for `Client`.
- Team state is match-local runtime state and should not be confused with `clans` group state.
- Champions role/build truth still belongs to `champions`, even though `game` exposes selectors and hotbar persistence around that setup.
- Hotbar layouts are persisted by `game`, but they only make sense relative to Champions roles and builds.

### State Layers Contributors Should Keep Separate

There are three linked but different layers:

- server state: current game, current map, current `GameState`, accepting players
- match state: teams, winners, attributes, `GameInfo`, mode-specific controllers, powerups
- player runtime state: participant alive/spectating state, selected team, selected role/build surfaces, inventory/application state

Bugs are easy when these layers are casually mixed.

## Integration Points

### `core`

`game` depends on `core` for:

- `ClientManager`
- shared player identity and gamer access
- combat and damage events
- stat increment infrastructure
- shared display helpers such as action bars, titles, and sounds
- command, listener, adapter, and update plumbing

### `champions`

`game` is strongly coupled to `champions` in the current code shape.

Visible current integration includes:

- role selection surfaces
- build selection menus
- hotbar layout persistence and application
- inventory refresh behavior tied to Champions build changes
- this server mode advertising itself as `CHAMPIONS`

This is an important integration, but `game` still should be documented as the match runtime rather than the combat owner.

### Maps And Region Metadata

The module depends on mapped world metadata and named regions for:

- selecting compatible maps
- locating spawn points
- placing team selectors
- placing kit selectors
- placing powerup mechanics and other game-mode surfaces

### Shared Stats

`game` feeds match context into shared stat flows through `GameInfo`, `StatManager`, and team snapshots.

This is a major contributor concern because stat correctness depends on the match context being recorded at the right times, not just on combat events firing.

## Current Game Modes

The document should treat the shared runtime and the current concrete modes with roughly equal importance.

### Shared Runtime

The reusable runtime layer is the part of `game` that every concrete mode inherits.

Current important shared systems include:

- the server-level state machine
- participant registration and capability updates
- team assignment and balancing
- waiting-lobby versus current-map handling
- match-scoped attributes
- game-scoped injector and listener lifecycle
- shared start and ending presentation
- shared stat context and persistence hooks

This layer is what makes `game` a module rather than just two disconnected minigames.

### `CaptureTheFlag`

`CaptureTheFlag` is a two-team flag mode layered onto the shared runtime.

Current defining behavior includes:

- blue versus red team structure
- respawn-enabled team match flow
- score-to-win through flag captures
- sudden death once the regular capture phase ends in a tie-sensitive state
- winner logic based on captures first, then alive teams in sudden death

Important contributor framing:

- CTF inherits the shared match loop from `TeamGame`
- its mode-specific identity comes from flag interaction, capture scoring, and sudden-death rules
- its own controller/listener flow is what turns the generic runtime into a flag game

### `Domination`

`Domination` is a two-team point-control mode layered onto the shared runtime.

Current defining behavior includes:

- blue versus red team structure
- score-to-win through accumulated points
- capture points as the main objective
- kill score and gem score as additional point sources
- gem powerup registration as a mode-specific runtime mechanic

Important contributor framing:

- Domination inherits the same shared match loop and team runtime
- its identity comes from capture-point control and score accumulation rather than flag movement
- its controller plus gem powerup registration are the current main mode-specific hooks

## Attached Subsystems

These systems are important and visible, but they should stay conceptually attached to the match runtime rather than redefining the module's primary identity.

### Team Selector NPCs

The waiting lobby uses team selector NPCs so players can pre-commit to teams before the live match starts.

### Role / Kit Selector NPCs

The module exposes Champions role/build setup through selector NPCs in the waiting lobby and current map surfaces.

### Champions Hotbar Layout Editor

`game` persists and applies Champions hotbar layouts for this server mode.

This is a major attached subsystem because it materially shapes player setup, but it still depends on Champions role/build truth.

### Team Chat And Spectator Chat

The module provides match-aware chat behavior for team and spectator communication.

### Player-List / Sidebar Presentation

Match runtime state is surfaced through tab color, sidebar, and related presentation helpers.

### Powerups

Powerups are match-local pickups or runtime bonuses that concrete modes can register and use.

### Spectate And Admin Commands

Commands such as spectate, team chat, spectator chat, and moderator game controls are operational surfaces around the core runtime.

### Achievements And Stat Surfacing

The module loads achievements and pushes match context into stat flows, but these remain downstream of the core match loop.

### Discord Game-Result Webhook

Ending-state webhook reporting is a presentation and observability surface layered onto match completion.

### Weather / World / Player Support Listeners

Many listeners exist to support the runtime by managing player state, interaction limits, world conditions, and transition behavior.

## Invariants And Easy-To-Break Rules

- `game` is the team-match runtime, not a generic pile of gameplay helpers.
- The server cannot leave `WAITING` without a current game assigned.
- The current game can only change while in `WAITING`.
- State transitions are intentionally narrow: `WAITING -> STARTING -> IN_GAME -> ENDING -> WAITING`.
- Game-scoped listeners, modules, and injectors only exist for the active game and must be torn down on exit.
- The waiting lobby is a required special-case map surface; startup is not valid without it.
- Match maps are loaded from zipped templates and must be compatible with the selected game's metadata expectations.
- `Participant` state is runtime-owned and distinct from shared player identity.
- Player capabilities and game mode are derived from both match state and participant state.
- Team runtime in `game` should not be confused with persistent clan state in `clans`.
- `GameInfo` is not just bookkeeping; stat correctness depends on switching cleanly between lobby and live-game records.
- Saving team-based game context may require offline client resolution during persistence, so stat writes are not purely synchronous runtime concerns.
- Hotbar layouts are owned by `game` in persistence terms, but they depend on Champions roles/builds being available.
