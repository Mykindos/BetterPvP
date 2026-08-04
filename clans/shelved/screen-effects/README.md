# Screen effects (recovery notes)

Server-driven camera roll, screen shake and the weather grades, drawn by the client's own core shaders — no mod and
no shader pack. It lived at `core/framework/shader/` in the plugin and at `assets/betterpvp/shaders/` plus a dozen
overridden core shaders in the resource pack. Both halves have been removed.

Only the shelved [discovery](../README.md) feature used it: `SteeringService` banked the horizon with the rudder and
shook the deck in a whirlpool.

## What is here

`classes/` — the compiled plugin half, lifted out of a build artifact before it was overwritten. The `.java` sources
were never committed and are gone; these decompile back to working code, minus the comments.

The shader half has to be re-derived from vanilla, which the notes below are enough to do.

## The technique

Core shaders take no server-supplied uniforms. There is no packet that means "roll the camera". What there *is* is a
number the server already sends per player and the client already hands to every shader: the **world age** on the
time-update packet, which arrives as the `GameTime` uniform.

So that field is the whole channel. The server packs a bitset and one argument into it:

```
payload = mask * 256 + argument      (0..23999, one Minecraft day)
```

- `mask` — which effects are on, one bit each, up to seven
- `argument` — a single number every enabled effect shares, 47 usable steps

The mask sits in the high place value because the client keeps counting game time forward on its own between packets.
That drift can only ever disturb the argument, never flip an effect on or off. The shader-side decode additionally
floors the payload to even ticks, which absorbs one whole tick of drift — and costs half the argument's resolution.

That last part is load-bearing on the server sending **even** values, which `ScreenEffectCodec.quantise` guarantees.
An odd payload floors down on frames where the client had not yet advanced its own clock and up on frames where it
had, so a continuously driven argument visibly shakes between two values at frame rate.

Two consequences worth knowing before rebuilding on this:

- **The shader has no memory.** Everything it knows arrives fresh each frame. There is no client-side interpolation,
  so anything that should ease has to be eased server-side and re-sent every tick.
- **The server owns the world age for everyone.** A player nobody has touched still has a *real* world age arriving,
  and a real world age decodes to an arbitrary bitset. Left alone, the horizon would start rolling on its own roughly
  every thirteen seconds, so every online player has to be pinned to a neutral zero.

## The pack half

`assets/betterpvp/shaders/include/` held three files: the payload layout and effect bit numbers (`bpvp_bus.glsl`),
the effects that move geometry — roll, shake, heat haze — and the effects that recolour — winter, desert, cave. The
bit numbers were a contract with `ScreenEffect.java`; changing one without the other silently plays a different effect.

Each overridden core shader was the vanilla 1.21.11 file with a single hook added around the `gl_Position` or
`fragColor` assignment, and nothing else changed: `terrain`, `entity`, `particle`, `sky`, `rendertype_beacon_beam`,
`rendertype_crumbling`, `rendertype_entity_shadow`, `rendertype_item_entity_translucent_cull`, `rendertype_lines`,
`rendertype_water_mask`, `block` and `rendertype_translucent_moving_block`. Re-deriving them after a Minecraft update
means starting from the new vanilla file.

`rendertype_text` was the exception: not a vanilla file but the pack's own HUD-anchor shader, which is **still in the
pack** because the HUD needs it. The effects hooked into it for world text only — name tags and holograms — since
rolling GUI text tips the chat and the scoreboard along with the horizon.

## Known limits

Inherited from the approach, not from the implementation:

- Sun, moon and stars are drawn outside these shaders and do not move with the effects.
- The lower half of the sky cannot be reached by a core shader at all.
- The first-person hand is an entity, so it is affected along with everything else. Telling it apart from a real
  entity means guessing from its distance to the camera, which gets it wrong both ways.
- Some modded clients need [sodium-core-shader-support](https://modrinth.com/mod/sodium-core-shader-support) for
  custom core shaders to run.

The technique is the one demonstrated by [Flaps](https://github.com/seailz/Flaps).
