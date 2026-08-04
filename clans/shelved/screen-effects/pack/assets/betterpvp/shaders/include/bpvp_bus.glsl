#ifndef BPVP_BUS
#define BPVP_BUS

// ============================================================================
// BetterPvP screen-effect bus -- shared decoder
// ----------------------------------------------------------------------------
// Core shaders have no uniform the server can write to, so the server smuggles
// a payload through the one number it already controls per player: the world age
// carried by the time-update packet. The client turns that into the `GameTime`
// uniform as `(gameTime % 24000 + partialTick) / 24000`, so a whole number in
// 0..23999 arrives here intact.
//
// Payload layout (kept in lockstep with ScreenEffectCodec.java):
//     payload = mask * 256 + arg
//   mask  0..93  -- bitset of active effects, one bit per effect
//   arg   0..92  -- a single shared parameter, normalised to 0..1 below
//
// Why the mask sits in the high place value: the client keeps counting game time
// forward on its own between packets, so the value seen here can be a tick or
// two ahead of what the server sent. A large stride means that drift can only
// ever disturb `arg`, never flip an effect on or off. `arg` is additionally
// floored to even ticks, which absorbs a single tick of drift completely -- so
// only ~46 of the 93 steps are actually usable, and that is fine.
//
// The cost of the whole scheme: the shader is stateless. Everything it knows
// arrives fresh each frame, there is no client-side interpolation, and any
// smoothing has to be done server-side by ramping `arg` over several ticks.
//
// This file is imported by both the vertex and the fragment library so the two
// halves can never disagree about the layout.
// ============================================================================

const float BPVP_DAY_TICKS = 24000.0;
const float BPVP_MASK_BASE = 256.0;
const float BPVP_ARG_MAX = 92.0;

// Effect bits. Mirror of ScreenEffect.java -- changing one without the other
// silently plays the wrong effect.
const int BPVP_EFFECT_ROLL = 0;
const int BPVP_EFFECT_SHAKE = 1;
const int BPVP_EFFECT_HEAT_HAZE = 2;
const int BPVP_EFFECT_WINTER = 3;
const int BPVP_EFFECT_DESERT = 4;
const int BPVP_EFFECT_CAVE = 5;

struct BpvpBus {
    int mask;
    float arg;    // 0..1
    float time;   // the raw GameTime, for effects that want a clock
};

BpvpBus bpvp_decode(float gameTime01) {
    float payload = clamp(floor(gameTime01 * BPVP_DAY_TICKS), 0.0, BPVP_DAY_TICKS - 1.0);
    payload = floor(payload * 0.5) * 2.0;

    float maskf = floor(payload / BPVP_MASK_BASE);
    float argi = clamp(payload - maskf * BPVP_MASK_BASE, 0.0, BPVP_ARG_MAX);

    BpvpBus bus;
    bus.mask = int(maskf + 0.5);
    bus.arg = argi / BPVP_ARG_MAX;
    bus.time = gameTime01;
    return bus;
}

bool bpvp_has(BpvpBus bus, int bit) {
    return ((bus.mask >> bit) & 1) != 0;
}

// The 0..1 argument read as -1..+1, for effects with a direction as well as a size.
float bpvp_signed(BpvpBus bus) {
    return bus.arg * 2.0 - 1.0;
}

#endif
