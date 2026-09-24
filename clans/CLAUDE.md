# Clans Module

The central gameplay module: clans, territory, relations, energy, world content, sailing and camps. Source root:
`clans/src/main/java/me/mykindos/betterpvp/clans/`.

## Where things live

| Package | What it holds |
|---|---|
| `clans/` | `Clan`, `ClanManager`, and subpackages for chat, core, insurance, pillage, protection, leveling, zone, map |
| `auctionhouse/` | Auction house UI (the backend is in `:shops`) |
| `combat/` | Clans combat rules |
| `database/` | Clan repositories |
| `world/resource/` | Harvestable POIs: ores, trees, fishing. Authoring guide in its README |
| `world/props/` | Data-driven props on any world. Authoring guide in its README |
| `world/residents/` | Data-driven NPCs from Mapper `npc_resident`/`npc_route`/`npc_display` points |
| `world/sailing/`, `world/ship/` | Voyages between sites. See `world/sailing/README.md` |
| `world/camp/` | A clan's own owned site |
| `world/aldenmark/`, `world/spawn/`, `world/veloran/` | Content bindings for specific places |

## Domain model

- `Clan` extends `PropertyContainer` and holds members, alliances, enemies, territory and insurance.
- `ClanMember` (in core, `core/components/clans/data/`) has a `MemberRank`: `RECRUIT`, `MEMBER`, `ADMIN`, `LEADER`.
- `ClanCore` (via `clan.getCore()`) handles energy, TNT recovery and the core boss bar.
- `ClanManager.isSafe(Location)` answers whether PvP is off at a spot.
- Territory is chunk based. `ClanManager` resolves chunk to clan.
- Clan state changes go through `ClanXxxEvent`s (e.g. `ClanPropertyUpdateEvent`) so listeners can react.

## World content

Content declares zones and scene objects from Mapper data and nothing else. Core's `WorldContentService` decides which
worlds it belongs to through a `WorldSelector` and owns load and teardown per world and per binding, so instanced worlds
work. Register with `contentService.register(clans, binding)`. `WorldSites.selector(id)` binds content to a site, and
`WorldSelector.any()` reaches every world. See `docs/SCENES.md`.

## Camps

`world/camp/` is an `OWNED` site whose owner is the clan id. `Camps` tells core which clan a player belongs to, what
their camp is built from and who is admitted. `Camp` is the record kept between visits and `CampStore` reads and writes
it through `SiteStorage`. The world is derived from a template and can be rebuilt at any time, so nothing that must
survive lives in its blocks. Player-facing camp management goes through in-world NPCs, never commands.

## Migrations

`clans/src/main/resources/clans-migrations/postgres/`
