# Resource Nodes — Authoring Guide

Resource nodes are harvestable points of interest (ore fields, trees, fishing ponds) placed in the world. This is the
generic framework that replaced the old Fields system.

**The model:** the *mechanic* is code (one archetype per kind — `ore`, `tree`, `fishing`), and each *node* is data — a
tagged Mapper region plus a YAML file. Adding "Birch Tree Lv40" or "Iron Mine Lv30" is a new tagged region (+ maybe one
YAML file), never new code.

To create content you touch three things:

1. A **Mapper data-point** (region) in the world, tagged so a node binds to it.
2. A **node YAML file** at `plugins/Clans/scenes/props/<name>.yml` describing the node.
3. External assets the node references — a **loot table** (in Supabase) and, for trees, **schematic files**.

After changes, reload with `/clans reload` (or restart). Defaults for the YAML live in
`clans/src/main/resources/configs/scenes/props/` and are copied to `plugins/Clans/scenes/props/` on first run.

---

## Common YAML fields (all archetypes)

```yaml
archetype: ore | tree | fishing   # which mechanic
match:                            # how this node binds to Mapper regions (pick ONE)
  name: copper_mine_1             #   a region with this exact name
profession: Mining                # Mining | Woodcutting | Fishing (capitalised). Drives XP. Optional.
level: 25                         # minimum profession level to harvest (gate). Default 0 (ungated).
displayName: "Copper Mine"        # label text. Default: the file name.
lootTable: copper_mine            # loot-table id resolved from Supabase (tree/fishing). Ore sets loot per-chain — see below.
respawn: 45                       # seconds before a harvested node restores, or none|never for a node that never does.
fixedSpeed: 60                    # optional: pin mining speed inside the node (scaled units). Omit for normal tools.
```

### Fixed mining speed (`fixedSpeed`)

By default a node is mined at whatever the held tool resolves to, so a netherite pickaxe strips it far faster than a
wooden one. Setting `fixedSpeed` pins the speed for **everyone inside the node** to that one value, whatever they are
holding — the rule is applied on entering the node's zone and withdrawn on leaving.

The value is in the block-break framework's scaled units (vanilla speed × 15), so the constants in `ToolMiningSpeed`
are the reference points: `30` mines like wood, `60` like stone, `90` like iron, `120` like diamond.

This is a **pacing** knob rather than a balance one: it lets a node be tuned so a full pass takes a known amount of
time regardless of the player's gear, which is what makes a starter area feel the same on a first login as it does to
a returning veteran. Omit the key (or set it to `0`) and the node mines normally.

### One-shot nodes (`respawn: none`)

`respawn` accepts either a delay in seconds or the sentinel `none`/`never` (case-insensitive), which marks the node
**one-shot**: harvested once and never restored. This is the setting for discovery-island content — a cloned island
world the player strips and leaves, where a node coming back would be wrong. An unparseable `respawn` value is logged
and falls back to the default delay rather than breaking the node.

One-shot composes with `parent:` — a shared template can stay on a normal respawn timer while an island variant of the
same node overrides it to `none`:

```yaml
parent: willow_tree
match: { name: willow_island }
respawn: none      # this placement never regrows; the willow_tree template it inherits from still does
```

Per archetype, one-shot means:

- **Tree** — fells normally and stays a stump forever.
- **Ore** — degrades/drops normally and never restores to its original material.
- **Fishing** — `respawn` is already unused here (fishing nodes don't consume blocks), so `respawn: none` is harmless
  and has no effect.

> Definitions are **world-agnostic**: a node binds to *every* matching region in *every* loaded world. Place the same
> tag in two worlds and you get a node in each — no `world` field to set.

### Region tags (set in Mapper, on the region)

- The **match tag** (e.g. `copper_mine`) — binds the region to the node file.
- `level:NN` — overrides `level` for *this* placement (e.g. one richer copper mine at `level:40`).
- `name:Some Name` — overrides `displayName` for this placement.

So one YAML file can drive many regions, each individually tuned by its own tags.

### Game mode

A node's zone puts everyone inside it in **survival**, in every world, because a node is by definition somewhere you
harvest. Outside any area that says otherwise players are in adventure, so a mine placed in the spawn world or on an
island is mineable for the same reason one in the wilderness is — you do not have to arrange anything for it.

What may actually be taken out of the node is still the node's decision (profession gate, chains, break rules); the
game mode only decides whether the client is allowed to swing at all.

If the *surroundings* of a node should also be survival (a starter area you want players to be able to build in, say),
that is a property of the surrounding zone: give its entry in `zones/<continent>.yml` a `gamemode: survival`.

### Labels

By default a node gets one auto-label floating above its centre. To place labels yourself, drop Mapper **point or
perspective** data-points tagged `label` **inside the node's region**. Each becomes a label; if you place several,
every player sees only the **nearest** one (handled efficiently by `ResourceNodeLabelService`). Because association is
full 3D containment, nodes can share x/z columns.

### Profession gate & XP

- A player below `level` is denied with a message; admins bypass (so they can build inside nodes).
- XP is awarded through the decoupled progression bridge (`ResourceNodeProgressionListener`), scaled by
  `resourcenodes.xpMultiplier` in `config.yml` (default `5.0`):
  - **Mining** — uses the harvested ore's configured XP from `progression`'s `mining.xpPerBlock`.
  - **Woodcutting / Fishing** — uses the node's flat `xp:` value (add an `xp: <number>` field).
- If the Progression plugin is absent, harvesting is ungated and grants no XP (fail-open).

### Loot

`lootTable` is an id looked up in the loot-table system (Supabase). It must exist there or drops are empty (the server
logs a warning). One loot table can be shared by many nodes. Energy shards, coins, and items are all loot-table entry
types, so "special" drops are just loot-table config.

For **tree** and **fishing** nodes the table is the node-level `lootTable`. **Ore** nodes set loot **per stage**
(`chains.<name>[].lootTable`), and a stage with no `lootTable` falls back to the block's **vanilla drops** — see the ore
section.

---

## Inheriting from a template (`parent`)

A node file can inherit from another via a `parent` field and override only the keys it sets, so families of similar
nodes don't repeat the same blocks. The parent is referenced one of three ways:

```yaml
parent: ore_base                 # by id (shorthand) — another file's name without .yml
parent: { id: ore_base }         # by id (explicit)
parent: { file: templates/ore_base.yml }   # by file, relative to scenes/props (the .yml is optional)
```

Resolution is a **deep merge**: nested blocks (e.g. the `chains:` map) merge key-by-key, while scalars and lists are
replaced wholesale. It **chains to any depth** — a parent may itself declare a `parent`.

```yaml
# scenes/props/templates/ore_base.yml  — a template; no `match`, so it never spawns on its own
archetype: ore
profession: Mining
respawn: 45
chains:                                     # default erosion rule; children add their own resource chains
  erosion:
    - stone
    - cobblestone
    - { material: deepslate, unbreakable: true }

# scenes/props/copper_mine.yml  — inherits the above, overrides only what differs
parent: templates/ore_base
match: { name: copper_mine }
level: 25
displayName: "Copper Mine"
chains:                                     # the map merges per-key: 'copper' is added, the parent's 'erosion' is kept
  copper:                                   # (restate a key to override that chain; its list replaces wholesale)
    - { material: copper_ore, lootTable: copper_mine }
    - stone
```

**Template-only files** (no `match` selector) are meant to be inherited, never spawned. Keep them in a **subfolder**
(e.g. `scenes/props/templates/`): files there are still indexed for `parent` resolution by id or file, but only
top-level files are considered for spawning, so a pure template never loads as a half-configured node. A missing parent
or a cyclic `parent` chain is logged and the dependent node is skipped.

---

## Ore nodes (`archetype: ore`)

Bind to a **cuboid** region. A node carries one or more **degrade chains** (`chains`); a chain is an ordered list
of material stages a block steps through when mined (`copper_ore → stone → cobblestone`). Ore nodes carry the `fields`
zone tag, so `ClanManager#isFields` keeps working inside them.

```yaml
archetype: ore
match: { name: copper_mine }
profession: Mining
level: 25
displayName: "Copper Mine"
respawn: 45
chains:
  copper:                           # the key is a readable label only
    - material: copper_ore          # a resource stage with a custom loot table; respawns to copper_ore
      lootTable: copper_mine
    - stone                         # a plain-string stage -> the block's VANILLA drops
  diamond: [diamond_ore, stone]     # a whole chain as a plain inline list (every stage drops vanilla)
  erosion:
    - stone                         # erosion: stone anywhere in the zone steps down...
    - cobblestone
    - material: deepslate           # ...to deepslate, which is un-mineable
      unbreakable: true
```

`chains` is a **map of named chains**; the key (e.g. `copper`) is a readable label that carries no behaviour, and each
value is a **list of stages**. A **stage** is either a plain material string or an object with:

| Key | Meaning |
|---|---|
| `material` | the block material for this stage |
| `lootTable` | *(optional)* loot-table id rolled when a block at **this stage** is mined; **omit it to drop the block's vanilla drops instead** |
| `unbreakable` | *(optional)* `true` makes a block at this stage un-mineable — the `BlockDamageEvent` is cancelled so there's no cracking animation (typically the last stage) |

Loot is therefore **per stage**: every mined block drops its own table, or its vanilla drops when the stage names no
table. A chain needs at least two stages (something to degrade to).

**Resource vs. erosion chains — what respawns.** At load the region is scanned once; a block is snapshotted (and so
respawns to its original after `respawn` seconds) only if its material is the first stage of a **resource** chain — one
whose first stage is *never* something another chain degrades into. Above, `copper_ore`/`diamond_ore` are resources
(snapshotted, respawn, drop loot); `stone` is the matrix they erode into, so the `stone → cobblestone → deepslate`
chain is a pure **erosion rule** that never respawns. That is what gives the "any stone in the zone turns to
cobblestone, then to permanent unbreakable deepslate" behaviour.

**Conflicting chains** are resolved case-by-case. When a block is mined, the chain it was originally snapshotted as
wins **if it still continues** from the current material; otherwise the first chain that *can* continue the current
material takes over. So a copper block degrades `copper_ore → stone` on its own chain, then picks up the stone chain
(`stone → cobblestone → deepslate`) once copper's chain is exhausted — while a block that was stone to begin with
follows the stone chain from the start.

**Steps:** build the ore blocks into the map → draw a cuboid region around the mine, tag it `copper_mine` → set
`mining.xpPerBlock` for the ore in the Progression config → create any loot tables you reference in Supabase.

*Global speed:* world events (Mining Madness) can multiply all ore respawn speed via `ResourceNodeSpeed`. A one-shot
node (`respawn: none`) is immune to this multiplier — it never restores no matter how it's tuned.

---

## Personal ore nodes (`archetype: personal_ore`)

The same mining model as `ore`, except **each player depletes the node separately**. Everyone sees it whole until they
mine it themselves, and each player's ore comes back on their own timer. Two people can stand in the same mine and see
different walls.

This is the archetype for a starter mine. A shared field is a race — arrive late and it is stripped — which is the
right pressure somewhere worth fighting over and the wrong first impression for someone who just logged in. It is the
wrong archetype for contested content, for exactly the same reason.

```yaml
archetype: personal_ore
match: { name: training_mine }
profession: Mining
level: 0
displayName: "Training Mine"
respawn: 300
fixedSpeed: 60                    # usually paired: every pickaxe mines this node at the same rate
chains:
  bloomstone:
    - material: copper_ore
      lootTable: training_mine
    - stone
  erosion:
    - stone
    - cobblestone
    - { material: deepslate, unbreakable: true }
```

`chains`, `level`, `profession` and `respawn` all mean exactly what they do for `ore`. Three things differ:

**The world block never changes.** It stays the intact ore forever, and each player is sent their own view of it. So
the mine always looks full to a fresh arrival, admin tooling and world edits see the real blocks, and there is no state
in the world to get out of step. It also means an operator looking at the mine is *not* seeing what any given player
sees — the only way to inspect that is to mine it yourself.

**A stage with no `lootTable` drops nothing**, where `ore` would fall back to the block's vanilla drops. Vanilla drops
come from the world block, which here is deliberately not what the player mined, so falling back would hand out intact
ore however far down the chain they actually were. Give every stage that should drop something a loot table; erosion
stages dropping nothing is usually what you want anyway.

**`respawn: none` is not supported.** A one-shot personal node would have to keep every player's rows forever. It is
logged and treated as a normal respawn. Use `ore` on an island world for one-shot content — the world is already
per-instance there, so it is per-player by construction.

### Where the state lives

In the database (`personal_mine_points`), one row per depleted point, not in a server-local cache. Two reasons, and
both matter:

- **A relog must not refill the mine.** There is no world state to re-read at load, so without a record the node comes
  back whole the moment a player reconnects — and a respawn timer you can skip by pressing quit is not a timer. The
  persistence is what makes the pacing real.
- **Spawn may be sharded.** A player who hops shards has to find the mine as they left it, and a file in one shard's
  data folder is invisible to the other.

Rows are read when a player *enters* the node and dropped from memory when they quit; anything already respawned while
they were away is cleared on the way in. Rows left by players who never come back are pruned after a day.


---

## Respawn timers

Every depleted ore block floats the time left until it comes back, on each of its **uncovered** faces, counting down as
`04m 32s` and running from red at the moment it was mined to green as it returns. This applies to `ore` and
`personal_ore` alike and needs no configuration to turn on.

Visibility follows the node. A shared node shows one countdown everybody reads, because everyone is waiting on the same
block. A `personal_ore` node shows each player their own, hidden from everyone else, because two people standing
together are waiting on different things.

Three things keep the entity count down: only uncovered faces get one (a block set in a wall shows one face, a fully
buried one shows none), the displays carry a short view range so the client stops drawing them beyond ~12 blocks, and a
per-player node draws only the 8 nearest points to each player.

### Turning it off for a chain (`timer`)

A chain can opt out, for regrowth not worth announcing — so a field does not become a wall of countdowns over material
nobody is waiting on. Erosion chains never respawn and so never had a timer to suppress; this is for **resource**
chains you would rather kept quiet.

A chain is written either as a bare list of stages, or as a block with its stages under `stages` and chain-level
options beside them. Both mean the same thing where they overlap, so nothing existing needs rewriting:

```yaml
chains:
  copper: [copper_ore, stone]     # a bare list — timers on, as normal
  gravel:                         # a block, for a chain that sets an option
    timer: false
    stages: [gravel, stone]
```


---

## Tree nodes (`archetype: tree`)

Bind to a **perspective** region (a point with a facing). The marker's **location is the paste anchor** and its **yaw
(snapped to 90°) is the rotation**. The tree is built entirely from authored schematics — you don't build each tree in
the world, you place a marker and the system pastes the tree there, rotated to the marker's facing. Hitting it
`tree.hits` times fells it through the stage schematics (last = stump) and rolls the loot table; after `respawn`
seconds the standing schematic is re-pasted (its own air clears the debris) — or, with `respawn: none`, the tree
simply stays a stump.

```yaml
archetype: tree
match: { name: willow }
profession: Woodcutting
level: 15
displayName: "Willow Tree"
lootTable: willow_tree
respawn: 120
xp: 12                    # flat woodcutting XP per fell (× resourcenodes.xpMultiplier)
tree:
  hits: 3                 # clicks/breaks to fell
  stageDelay: 6           # ticks between fell frames
  standing: willow_standing            # standing tree schematic
  stages:                              # fell animation; last = stump
    - willow_fall_1
    - willow_fall_2
    - willow_stump
```

**Authoring the schematics** (in `plugins/Clans/schematics/`, FAWE-readable `.schem`/`.schematic`):

1. Build the tree once in a canonical facing.
2. Stand **on the trunk base** and `//copy` — the `//copy` origin becomes the block that lands on the marker. Save as
   `willow_standing.schem`. Author it over the tree's full footprint (including air) so regrow clears debris.
3. Build/`//copy` each fell frame the same way (same origin), ending with the stump. Keep debris within the standing
   footprint so regrow clears it.

**Placing trees:** drop a **perspective** Mapper data-point tagged `willow` wherever you want a tree, facing the
direction it should fell. Repeat for as many trees as you like — one schematic set, many markers, any orientation. Tag
`level:`/`name:` to tune individual trees.

> Note: rotation snaps to 90°. If a placed tree looks mirrored or off by a quarter-turn, that's a handedness tweak in
> `SchematicAnimator` — report what you see.

---

## Fishing nodes — ponds & lava ponds (`archetype: fishing`)

Bind to a **cuboid** region covering the water (or lava). When a player reels in a catch whose hook is inside the zone,
the vanilla fish is removed and the node's loot table is rolled instead, so the pond drops whatever you configure.

```yaml
archetype: fishing
match: { name: trout_pond }
profession: Fishing
level: 10
displayName: "Trout Pond"
lootTable: trout_pond
respawn: 0                # fishing nodes don't consume blocks; respawn is unused
xp: 8                     # flat fishing XP per catch (× resourcenodes.xpMultiplier)
fishing:
  liquid: water           # 'water' (default) or 'lava'
```

**Lava ponds:** set `liquid: lava` and point `lootTable` at a lava-themed table. The loot-replacement flow is identical
to water. Note: vanilla bobbers cannot physically settle in lava, so the *casting* mechanic for a hook to reach a
"caught" state over lava is a separate feature not handled by this archetype.

**Steps:** draw a cuboid around the water, tag it `trout_pond` → create the `trout_pond` loot table in Supabase (use
the `fish`/item entry types).

---

## Quick reference

| Need | Where |
|---|---|
| Add/spawn a node | tag a Mapper region + a `scenes/props/<name>.yml` |
| Share config across nodes | a `parent:` template (deep-merged; template-only files in a subfolder) |
| Per-placement tuning | region tags `level:NN`, `name:...` |
| Per-player depletion | `archetype: personal_ore` |
| One speed for every pickaxe | `fixedSpeed:` on the node |
| Quieten a chain's countdowns | `timer: false` on the chain |
| Drops | a loot table by id in **Supabase** |
| Tree visuals | `.schem` files in `plugins/Clans/schematics/` |
| Mining XP per ore | Progression `mining.xpPerBlock` |
| Tree/fishing XP | the node's `xp:` field |
| Global XP scale | `resourcenodes.xpMultiplier` in clans `config.yml` |
| Apply changes | `/clans reload` |

Code lives in `clans/world/resource/` (framework + `archetype/`) and `core/world/schematic/` (schematic subsystem).
