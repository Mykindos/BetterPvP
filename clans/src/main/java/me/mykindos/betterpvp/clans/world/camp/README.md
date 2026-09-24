# Camps

`world/camp/` is a clan's own place, reached by ship like anywhere else. It is an `OWNED` site whose owner is the
clan id, so the site framework works in numbers throughout.

- `Camps`: the whole of what core is told: which clan a player belongs to, what their camp is built from, and the
  `clan` admission rule. Allies are admitted, because a crew sails as one party and is admitted or refused as one,
  so members-only would strand an ally at sea rather than merely turning them away.
- `Camp`: what is kept between visits: the skin, the `Holding` (core construction's structures and jobs), the
  Wood/Stone/Iron balance and the clan's own rank permissions (null until changed, then a copy of the defaults).
- `CampStore`: reads and writes those records through whatever `SiteStorage` is bound. A
  record is read once, on join. Anything that changes a camp calls `changed(clanId)`, `Camps` flushes changed records
  every 5 seconds, and `Clans.onDisable` waits for a final flush.
- `CampConstruction`: the camp's `ConstructionSite`: holding, ledger, permissions, the tier gate (tier N needs a
  Great Hall at version N, versions counting from 0) and the overflow drop after a demolish.
- `CampConfig`: `configs/camps.yml`: chest capacity, deposit values by item key, overflow items, claim layers and
  default rank permissions.
- `resource/`: `CampResources` (the ledger), `ResourceChests` (Mapper points named `resource_chest` in a structure's
  build), `ResourceChestDeposit` (right-click a resource chest to deposit everything it takes, refused whole if over
  capacity) and `ResourceOverflow`.
- `structure/`: `CampStructures` registers every camp structure (code: id, tier, requirements, flags) as a
  `CampStructure` whose numbers (versions with build, cost and time, move, repair, demolish refund, icon) come from
  `camps.yml` `structures.<id>`. `StartingCamp` seeds an empty holding from the skin's `camp_start` perspective
  markers tagged `structure:<id>` (the Dock starts broken).
- `menu/`: `ConstructionMenu` (availability via `ConstructionService.unavailable`, hands out blueprints) and
  `CampPermissionsMenu` (leader edits, leader row always allowed). Players reach both only through the Steward.
- `hall/`: the **Steward**, one NPC on the Great Hall's `steward` point. Right-click opens `GreatHallMenu`, the hub
  for everything a clan manages: Settlers (roster), Wages, Crews, Construction, Permissions, nested with `BackButton`.
  **No player commands for camp management**; new features get a page under the Steward.
- `settler/`: camp side of core `world/settler/`: `CampSettlers` (the `SettlerSite`), `CampBuilders`, `CampMorale`,
  `CampWageFund`, `SettlerConfig` (`settlers.yml`), `StarterCrew`, crew menus and settler cards. `/settler` is staff only.
- `protection/`: `CampGrounds` covers the camp world with one zone (priority 1, adventure) whose `CampGroundsRule`
  denies breaking and placing and lets only members open containers, plus one zone over `farm` cuboids (priority 2,
  survival for members) where members plant and harvest crops. Resource nodes and the dock sit above both and keep
  their own rules. `CampProtectionListener` stops explosions, fire, decay, mobs, trampling and bone meal changing the
  land, and sends the denial message.
- `CampRespawn`: every clan member respawns at their own camp's Barracks (the build's `respawn` point, broken or
  not), wherever they died. A camp loaded here takes them straight there; otherwise they respawn normally and are
  sent with `Placement.send(player, handle, "barracks")`, a named landing core's `SiteLandings` resolves on the
  camp's server. `CampArrivalNotices` lists structures ready to claim, needing repair or disabled when a member
  arrives. Allies get their own row in the permissions menu (actions plus container access), stored on `Camp`.
- Structures grow through **stages** (`StructureStage`, `advance`). **Upgrades** are the one-of-several choice per
  stage: core `StructureUpgrade`, `ConstructionService.upgrade`, `FIT_UPGRADE` jobs, pieces on `upgrade:<id>` points.
  Each effect class in `world/camp/upgrade/` declares its upgrade through `CampUpgrades.declare` and checks
  `CampUpgrades.has`; numbers in `camps.yml` `structures.<id>.upgrades.<id>`. Old records with `version`/`UPGRADE`
  still read through `@JsonAlias` (UPGRADE = advance).
- Item storage is core's `StructureStorage`: every chest, trapped chest and barrel in a structure's build is kept on
  its `PlacedStructure.storage`, keyed by position in the build. `StructureView` fills a container when its layer
  goes up, writes it down on inventory close and before its layer comes down, and a finished build moves contents of
  containers it no longer has into the rest (dropping overflow). Demolish drops the lot.
- `CampContent`: `CampGrounds`, the dock, core's `BuildZones` (Mapper cuboids named `build_zone`), `StartingCamp`,
  then core's `StructureViews`, in that order.

**The world is derived, the record is not.** A camp world is built from a template and can be rebuilt from one at any
time, so nothing that has to survive that lives in its blocks.

Design and phased plan in Outline under **Engineering**.
