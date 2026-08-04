# Screen effects

Server-driven visual effects — the horizon banking, the screen shaking, a blizzard, desert heat — drawn by the
client's own core shaders. No mod, no shader pack: a player with this resource pack loaded has them, and a player
without it sees the world exactly as they always did.

Driven from the plugin by `me.mykindos.betterpvp.core.framework.shader.ScreenEffectService`.

## How the server says anything at all

Core shaders take no server-supplied uniforms. There is no packet that means "roll the camera". What there *is* is a
number the server already sends per player and the client already hands to every shader: the **world age** on the
time-update packet, which arrives as the `GameTime` uniform.

So that field is the whole channel. The server packs a bitset and one argument into it:

```
payload = mask * 256 + argument      (0..23999, one Minecraft day)
```

- `mask` — which effects are on, one bit each, up to seven
- `argument` — a single number every enabled effect shares, ~46 usable steps

The mask sits in the high place value because the client keeps counting game time forward on its own between packets.
That drift can only ever disturb the argument, never flip an effect on or off. `bpvp_decode` additionally floors the
payload to even ticks, which absorbs one whole tick of drift — and costs half the argument's resolution.

Two consequences worth knowing before building on this:

- **The shader has no memory.** Everything it knows arrives fresh each frame. There is no client-side interpolation,
  so anything that should ease has to be eased server-side and re-sent every tick.
- **The server owns the world age for everyone.** A player nobody has touched still has a *real* world age arriving,
  and a real world age decodes to an arbitrary bitset. Left alone, the horizon would start rolling on its own roughly
  every thirteen seconds. `ScreenEffectService` therefore pins every online player to a neutral zero.

## Files

| File | What it is |
|---|---|
| `include/bpvp_bus.glsl` | The payload layout and the effect bit numbers. Imported by both halves below. |
| `include/bpvp_effects_vertex.glsl` | Effects that move geometry: `ROLL`, `SHAKE`, `HEAT_HAZE`. |
| `include/bpvp_effects_fragment.glsl` | Effects that recolour: `WINTER`, `DESERT`, `CAVE`. |

The bit numbers in `bpvp_bus.glsl` are a contract with `ScreenEffect.java`. Changing one without the other silently
plays a different effect.

## Which core shaders are overridden

Each override is the vanilla 1.21.11 file with a single hook added, and nothing else changed — so re-deriving them
after a Minecraft update means starting from the new vanilla file, not from these.

`terrain`, `entity`, `particle`, `sky`, `rendertype_beacon_beam`, `rendertype_crumbling`, `rendertype_entity_shadow`,
`rendertype_item_entity_translucent_cull`, `rendertype_lines`, `rendertype_water_mask`, and `rendertype_text`.

`rendertype_text` is the exception: it is not a vanilla file but the pack's own HUD-anchor shader, which the effects
hook into for **world text only** (name tags, holograms). Rolling GUI text would tip the chat and the scoreboard along
with the horizon.

## Adding an effect

1. Claim a free bit in `bpvp_bus.glsl` (0–6) and add the matching constant to `ScreenEffect.java`.
2. Write the effect in whichever half suits it, and call it from that file's `bpvp_apply_*_effects` entrypoint.
3. If it needs a shader that is not overridden yet, copy the **vanilla 1.21.11** file, add the import, and wrap the
   `gl_Position` or `fragColor` assignment.

Every effect shares the one argument, so two effects that both need steering cannot be steered independently at once.

## Known limits

Inherited from the approach, not from this implementation:

- Sun, moon and stars are drawn outside these shaders and do not move with the effects.
- The lower half of the sky cannot be reached by a core shader at all.
- The first-person hand is an entity, so it is affected along with everything else. Telling it apart from a real
  entity means guessing from its distance to the camera, which gets it wrong both ways.
- Some modded clients need [sodium-core-shader-support](https://modrinth.com/mod/sodium-core-shader-support) for
  custom core shaders to run.

The technique is the one demonstrated by [Flaps](https://github.com/seailz/Flaps).
