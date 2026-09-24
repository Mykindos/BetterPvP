# Core Module

The shared foundation every other module depends on. Source root: `core/src/main/java/me/mykindos/betterpvp/core/`.

## Where things live

| Package | What it holds |
|---|---|
| `framework/` | `BPvPPlugin`, loaders, `@BPvPListener`, `delayedactions/` (being retired) |
| `framework/net/` | Cross-server `MessageBus`, `SiteDirectory`, `PlayerTransfer`. See its README |
| `config/` | `@Config` injection, `ExtendedYamlConfiguration` |
| `database/` | `Database`, jOOQ context, repositories |
| `client/` | `Client` (persistent identity) and `Gamer` (realm-scoped state) |
| `locale/` | `Translations` |
| `item/`, `anvil/`, `repair/`, `imbuement/`, `recipe/` | Component-based custom items and crafting |
| `access/` | Item capability gating by scope. See its README |
| `menu/` | GUI framework. `menu/navigation/` is the generic destination picker |
| `scene/` | Scene objects, NPCs, props, custom mobs |
| `cutscene/` | Camera and dialogue tracks. See its README |
| `quest/` | Quest and conversation runtime |
| `world/site/` | Places a player can be: sites, instances, residency, crews. See its README |
| `world/content/` | `WorldContentService`, `WorldSelector`, per-world content binding |
| `world/zone/`, `world/terrain/`, `world/schematic/`, `world/mapper/` | Zones, terrain scan, FAWE schematics, Mapper data |
| `scheduler/` | `@UpdateEvent` dispatch |
| `effects/`, `cooldowns/`, `energy/`, `combat/` | Shared combat systems |
| `utilities/` | `UtilMessage`, `UtilServer`, `UtilTime`, `UtilItem`, `UtilBlock`, `UtilEntity`, `UtilPlayer` |

## Invariants other modules can rely on

- Shared player identity is `Client`. Realm-scoped gameplay state hangs off `Gamer`. Don't invent parallel player models.
- Rank and punishments are shared, cross-module concepts.
- Property and stat persistence originates in core. Changes happen in memory and are flushed in batches.
- `@UpdateEvent` methods only run on classes `ListenerLoader` registered.

## Config and migrations

- Config YAMLs live under `core/src/main/resources/configs/`.
- Migrations: `core/src/main/resources/core-migrations/postgres/V{YYYYMMDD}_{n}__{Description}.sql`.
- Translations: `core/src/main/resources/translations/core_<lang>.properties`, 12 files, all updated together.
