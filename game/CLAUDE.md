# Game Module

A mini-game framework that runs one game mode at a time on a dedicated server. Source root:
`game/src/main/java/me/mykindos/betterpvp/game/`.

## Where things live

| Package | What it holds |
|---|---|
| `framework/AbstractGame`, `TeamGame` | Base game types |
| `framework/GameRegistry` | Game type registry |
| `framework/ServerController` | Which game is running, starting and cycling games |
| `framework/state/` | `GameStateMachine` over `GameState`: `WAITING`, `STARTING`, `IN_GAME`, `ENDING` |
| `framework/model/` | Players, teams, spawn points, attributes, settings, stats, worlds |
| `framework/module/powerup/` | Powerups |
| `framework/configuration/` | Per-game and per-map configuration |
| `impl/ctf/`, `impl/domination/` | Capture the Flag and Domination |

## Adding a game mode

Extend `TeamGame` (or `AbstractGame`) under `impl/<name>/`, register it in `GameRegistry`, and add a configuration class
if the map needs settings. See `CONTEXT.md` for the lobby, match and rotation lifecycle.

## Migrations

`game/src/main/resources/game-migrations/postgres/`
