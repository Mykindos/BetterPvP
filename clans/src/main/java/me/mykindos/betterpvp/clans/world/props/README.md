# Props

Scenery and clickable objects, authored as map data. A prop is one `prop` marker and its tags — no class, no config
file, no restart-the-plugin loop.

Props belong to **every** world. Draw the markers and they appear, in a hand-built town or in every instance cloned
from an island template. The same is true of [residents](../residents/Residents.java), which are the walking, talking
counterpart.

## The data point

`prop` — a **perspective** marker (facing matters). Every tag is optional.

### Identity
| Tag | Meaning |
|---|---|
| `id:` | Names this placement, so code answering its clicks can tell it from its twin on another island |
| `type:` | Names a `PropArchetype` — code behind the prop, for the few that need any |

### Look
| Tag | Default | Meaning |
|---|---|---|
| `model:` | none | ModelEngine blueprint. **Omit for an invisible prop** |
| `skin:` | none | Blueprint remapped over the model |
| `idle:` | none | Animation it rests in |
| `size:` | `1.0` | How large the model renders. `0.5` is half scale, `2` is double |
| `color:` | none | Tints the model — see below |
| `hitbox:` | `1.0` | How big it is to click |

`size:` scales the model only; `hitbox:` is separate on purpose, so a prop can be rendered small and still be
comfortable to click.

#### `color:`

A comma-separated list of `<colour>` and `<bone>=<colour>` entries. A bare colour is the default every bone takes; a
named bone overrides it.

```
color:ff8800                   whole model orange
color:black                    whole model black
color:flame=ffdd66             only the 'flame' bone; everything else keeps its texture
color:66aa88,flame=ffdd66      verdigris frame, warm flame
```

Each colour is hex (`#ff8800` or `ff8800`) or one of the sixteen vanilla colour names (`red`, `light_purple`,
`black`). Bone names are case-insensitive and are the bone ids from the Blockbench model.

Tint **multiplies** the texture rather than replacing it, so it darkens: `black` renders black, and a mid-grey renders
roughly half as bright. To keep part of a model at its authored colours, leave it out — an untinted bone is not the
same as a white one.

Only renderer bones are tinted; hitboxes and mount points are skipped. A bone name the model doesn't have is logged as
a warning, since it otherwise just quietly keeps its old colour. `color:` is applied after `skin:`, so the two compose.

A prop with no `model:` is an invisible, clickable marker. That is the right shape for a hotspot on something already
built out of blocks — a lectern, a door, a notice board — where the build *is* the visual.

### Label
| Tag | Meaning |
|---|---|
| `name:` | Floats above the prop |
| `subtitle:` | Smaller grey line above the name |
| `bone:` | Pins the label to a model bone so it follows animations |

### Clicks
| Tag | Meaning |
|---|---|
| `interact:` | Names a registered `SceneInteraction` |
| `interact_animation:` | Played when clicked |

`interact:` is the **only** way a prop becomes clickable — archetypes do not grant it. This is deliberate: being
clickable also suppresses block placement and item use for anyone looking at the prop, which would be wrong for
scenery you happen to be building next to.

### Ambience
| Tag | Default | Meaning |
|---|---|---|
| `radius:` | `32` | Nothing below is computed at all unless a player is this close |
| `sound:` | none | `key` or `namespace:key` — `sound:block.bell.use`, `sound:betterpvp:ship_creak` |
| `sound_interval:` | `100` | Ticks between plays |
| `sound_volume:` / `sound_pitch:` | `1.0` | |
| `particle:` | none | Particle name |
| `particle_interval:` | `10` | Ticks between puffs |
| `particle_count:` | `3` | Particles per puff |
| `particle_spread:` | `0.2` | How far they scatter, in blocks |
| `particle_height:` | `1.0` | How far above the marker they come from |

### Motion
| Tag | Default | Meaning |
|---|---|---|
| `spin:` | `0` | Degrees per second; negative spins the other way |
| `bob:` | `0` | How far it floats up and down, in blocks |
| `bob_period:` | `4` | Seconds for one full rise-and-fall |

Props do not walk. Motion here moves the *model*, never the prop's anchor — which is what keeps it re-appearing in the
right place after a chunk cycle. Something that should genuinely walk a route is a resident with a `route:` tag.

## Examples

A creaking, swaying ship's lantern:

```
prop
  model:scene_lantern
  sound:betterpvp:rope_creak
  sound_interval:140
  bob:0.15
  bob_period:6
  radius:24
```

An invisible hotspot on a built lectern:

```
prop
  id:town_notices
  name:Notice Board
  subtitle:Read the postings
  interact:notices
  hitbox:1.5
```

Content with only one possible meaning does not go through here at all — a ship's wheel is its own `ship_helm`
data-point, so a vessel cannot be authored with the tags mistyped and end up unsailable.

## When you need code

Two seams, and they are separate on purpose.

**A click that does something** — claim a name on `SceneInteractionRegistry`:

```java
sceneInteractions.register("notices", (player, placement) -> openNoticeBoard(player, placement.getWorld()));
```

The `placement` is how you tell one copy from another: it carries the marker's `id:`, all of its tags, its location and
the world it stands in. On instanced worlds this is not optional — two islands cloned from one template hold
byte-identical markers.

**Behaviour with no click** — claim a name on `PropArchetypeRegistry`:

```java
archetypes.register("brazier", (prop, placement) -> prop.addBehavior(new MyFlareBehavior(prop)));
```

`decorate` runs on **every** materialization, so it must build fresh behaviours rather than hold onto ones from last
time. A chunk cycle destroys the entity and everything attached to it; the prop itself survives.

## Validation

Mistakes here are silent — a prop authored with the wrong wand is dropped before its tags are read, an unknown `type:`
leaves it inert, a misspelt particle simply never appears, and all of them look like a prop that just sits there.
`PropValidator` reports them in the Mapper editor while you are still standing next to the marker. Use it.
