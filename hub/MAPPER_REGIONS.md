# Hub Mapper Regions

This is the complete set of named regions consumed by the `hub` module from `dataPoints.json`.

There are no additional dynamic region lookups in `hub` beyond the entries below.

| # | Region name             | Type | Required? | Description / uses |
|---|-------------------------|---|---|---|
| 1 | `spawnpoint`            | `PerspectiveRegion` | Yes | Main hub spawn location. Used as the authoritative hub spawn and as the fallback teleport when a player leaves the hub bounds. |
| 2 | `max_bounds`            | `PointRegion` | Yes | One corner of the hub bounding box. Used with `min_bounds` to define the playable area limit. |
| 3 | `min_bounds`            | `PointRegion` | Yes | Opposite corner of the hub bounding box. Used with `max_bounds` to define the playable area limit. |
| 4 | `FFA`                   | `PolygonRegion` | Yes | The hub FFA arena region. Used for zone detection, enter/exit rules, combat-leave prevention, and wall generation. |
| 5 | `ffa_spawnpoint`        | `PerspectiveRegion` | Yes | FFA respawn/spawn location. Also passed to the trainer NPC as the teleport destination. |
| 6 | `npc_store`             | `PerspectiveRegion` | Yes | Location for the store NPC. |
| 7 | `npc_trainer`           | `PerspectiveRegion` | Yes | Location for the trainer NPC. |
| 8 | `npc_selector_classic`  | `PerspectiveRegion` | Yes | Location for the Classic server selector NPC. |
| 9 | `kit_selector_assassin` | `PerspectiveRegion` | Yes | Kit selector location for the `ASSASSIN` role. |
| 10 | `kit_selector_knight`   | `PerspectiveRegion` | Yes | Kit selector location for the `KNIGHT` role. |
| 11 | `kit_selector_brute`    | `PerspectiveRegion` | Yes | Kit selector location for the `BRUTE` role. |
| 12 | `kit_selector_ranger`   | `PerspectiveRegion` | Yes | Kit selector location for the `RANGER` role. |
| 13 | `kit_selector_mage`     | `PerspectiveRegion` | Yes | Kit selector location for the `MAGE` role. |
| 14 | `kit_selector_warlock`  | `PerspectiveRegion` | Yes | Kit selector location for the `WARLOCK` role. |
| 15 | `npc_selector_squads`   | `PerspectiveRegion` | Optional | Optional server selector NPC for Squads. The code is present but commented out. |
| 16 | `npc_selector_casual`   | `PerspectiveRegion` | Optional | Optional featured server selector NPC for Casual. The code is present but commented out. |

## Notes

- `FFA` is expected to have child `CuboidRegion`s. The wall-generation logic iterates `region.getChildren()` as cuboids.
- Those FFA child cuboids are not looked up by name, so they are not separate named entries in the table.

## Code references

- [HubWorld.java](/A:/Projects/BetterPvP/BetterPvP/hub/src/main/java/me/mykindos/betterpvp/hub/model/HubWorld.java)
- [FFARegionService.java](/A:/Projects/BetterPvP/BetterPvP/hub/src/main/java/me/mykindos/betterpvp/hub/feature/ffa/FFARegionService.java)
- [HubNPCFactory.java](/A:/Projects/BetterPvP/BetterPvP/hub/src/main/java/me/mykindos/betterpvp/hub/feature/npc/HubNPCFactory.java)
- [Role.java](/A:/Projects/BetterPvP/BetterPvP/core/src/main/java/me/mykindos/betterpvp/core/components/champions/Role.java)
