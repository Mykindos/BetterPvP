# Screen effects

Server-driven camera roll, screen shake and the weather grades, drawn by the client's own core shaders — no mod and
no shader pack. It lived at `core/framework/shader/` in the plugin and at `assets/betterpvp/shaders/` plus the core
shaders it overrode in the resource pack. Both halves have been taken out of the running build.

Its only consumer was the shelved [discovery](../README.md) feature: `SteeringService` banked the horizon with the
rudder and shook the deck in a whirlpool. That coupling is three call sites — `screenEffects.drive(...)` in
`SteeringCues`, `screenEffects.clear(...)` in `SteeringService`, and the constructor parameter threaded to both — so
discovery can be restored without this and given its camera work back later.

`pack/assets/betterpvp/shaders/README.md` is the shader side's own documentation: the payload layout, why the world
age is the only channel there is, and how to add an effect. Read that first — everything below is only about the
state of what survives.

## What is here

```
pack/assets/betterpvp/shaders/include/   the three GLSL files: the payload layout, and the two effect halves
pack/assets/minecraft/shaders/core/      the overridden core shaders, each vanilla + one hook
classes/                                 the compiled plugin half
```

The pack files are the last built state (3 Aug 2026, 14:56). Two overrides are **missing** and have to be re-derived
from vanilla before this is whole: `block.vsh` and `rendertype_translucent_moving_block.vsh`, which were written after
that build and are gone. Each is the vanilla 1.21.11 file with the same single hook the others carry — copy the shape
from `terrain.vsh`.

`rendertype_text.vsh` here is the **hooked** version. The live pack still has this file, reverted to its HUD-anchor-only
form, because the HUD needs it. Restoring means putting the effects hook back into the live file, not overwriting it —
the HUD anchor and the effects hook both belong in it.

`classes/` is the plugin half, lifted out of a build artifact. Its `.java` sources were never committed and are gone;
these decompile back to working code, minus the comments. What the comments said is in the pack README and in the
memory note `project_screen_effect_shaders`.
