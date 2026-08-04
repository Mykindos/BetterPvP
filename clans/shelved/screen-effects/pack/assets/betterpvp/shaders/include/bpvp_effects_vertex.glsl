#ifndef BPVP_EFFECTS_VERTEX
#define BPVP_EFFECTS_VERTEX

#moj_import <betterpvp:bpvp_bus.glsl>

// ============================================================================
// BetterPvP screen effects -- vertex half
// ----------------------------------------------------------------------------
// Effects that move geometry: the camera banking, the scene shaking, air
// bending what is behind it. Each takes a clip-space position and returns a
// displaced one, so every core shader that draws world geometry can opt in with
// a single line before writing gl_Position.
//
// See bpvp_bus.glsl for how the server reaches these at all.
// ============================================================================

// ----------------------------------------------------------------------------
// ROLL -- the horizon tips, as if the camera itself were banking.
// ----------------------------------------------------------------------------

const float BPVP_ROLL_MAX_RAD = radians(16.0);
const float BPVP_ROLL_DEAD_ZONE = 0.03;
const float BPVP_ROLL_SOFT = 0.10;

// Quantising `arg` means the smallest step the server can send is a visible jump
// near zero, where the eye is most sensitive to it. Fading the response in over
// the first tenth of the range hides that step behind a ramp.
float bpvp_roll_response(float signed) {
    float x = clamp(signed, -1.0, 1.0);
    float magnitude = abs(x);

    float gate = smoothstep(BPVP_ROLL_DEAD_ZONE, BPVP_ROLL_SOFT, magnitude);
    float linear = clamp((magnitude - BPVP_ROLL_DEAD_ZONE) / max(1e-4, 1.0 - BPVP_ROLL_DEAD_ZONE), 0.0, 1.0);

    return sign(x) * pow(linear, 1.1) * gate;
}

// Clip space is not isotropic: x and y both span -w..+w, but the viewport is wider than it is tall, so one NDC unit
// of x covers more pixels than one of y. Rotating clip.xy directly therefore skews the image instead of turning it --
// on a 16:9 screen a "16 degree roll" arrives as a shear. Scaling x into the same pixel scale as y before the
// rotation, and back out after, makes it an actual rotation on screen.
vec4 bpvp_apply_roll(vec4 clip, BpvpBus bus) {
    float angle = bpvp_roll_response(bpvp_signed(bus)) * BPVP_ROLL_MAX_RAD;
    float s = sin(angle);
    float c = cos(angle);

    float aspect = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec2 square = mat2(c, -s, s, c) * vec2(clip.x * aspect, clip.y);

    clip.x = square.x / aspect;
    clip.y = square.y;
    return clip;
}

// ----------------------------------------------------------------------------
// SHAKE -- the whole scene jitters. Amplitude scales with `arg`, so a distant
// tremor and a hard impact are the same effect at different strengths.
// ----------------------------------------------------------------------------

const float BPVP_SHAKE_MAX_NDC = 0.015;

vec2 bpvp_shake_jitter(float dayTicks) {
    // Mixed incommensurate frequencies, so the pattern never settles into a
    // rhythm the eye can predict.
    float jx = sin(dayTicks * 37.0) + sin(dayTicks * 71.0 + 1.3);
    float jy = sin(dayTicks * 41.0 + 2.1) + sin(dayTicks * 89.0 + 0.7);
    return 0.5 * vec2(jx, jy);
}

vec4 bpvp_apply_shake(vec4 clip, BpvpBus bus) {
    // A shake sent with no argument at all should still be visible, so the
    // strength floors rather than fading to nothing.
    float strength = max(bus.arg, 0.25);
    vec2 jitter = bpvp_shake_jitter(bus.time * BPVP_DAY_TICKS) * BPVP_SHAKE_MAX_NDC * strength;

    // Same anisotropy as the roll: an equal offset in x and y is a wider push
    // sideways than upward, so x is divided back down to match.
    float aspect = ScreenSize.x / max(ScreenSize.y, 1.0);
    clip.xy += vec2(jitter.x / aspect, jitter.y) * clip.w;
    return clip;
}

// ----------------------------------------------------------------------------
// HEAT HAZE -- warm air bending what is behind it. Deliberately tiny: this is
// the geometry half of the desert grade in the fragment library, and anything
// larger reads as the world wobbling rather than the air above it.
// ----------------------------------------------------------------------------

float bpvp_wave(float x) {
    return sin(x) * 0.6 + sin(x * 1.7) * 0.3 + sin(x * 2.9) * 0.1;
}

vec4 bpvp_apply_heat_haze(vec4 clip, BpvpBus bus) {
    float t = bus.time * 2000.0;
    vec2 ndc = clip.xy / max(clip.w, 1e-5);

    // Only distant geometry ripples -- there is not enough near air to bend light.
    float depth01 = clamp((clip.z / max(clip.w, 1e-5)) * 0.5 + 0.5, 0.0, 1.0);
    float strength = smoothstep(0.35, 1.0, depth01) * 0.0020;

    float wx = (bpvp_wave(ndc.y * 18.0 + t) + bpvp_wave(ndc.y * 7.0 - t * 1.3)) * strength;
    float wy = bpvp_wave(ndc.x * 10.0 + t * 0.7) * strength * 0.35;

    clip.xy += vec2(wx, wy) * clip.w;
    return clip;
}

// ----------------------------------------------------------------------------

vec4 bpvp_apply_vertex_effects(vec4 clip, float gameTime01) {
    BpvpBus bus = bpvp_decode(gameTime01);
    if (bus.mask == 0) {
        return clip;
    }

    vec4 result = clip;
    if (bpvp_has(bus, BPVP_EFFECT_ROLL)) result = bpvp_apply_roll(result, bus);
    if (bpvp_has(bus, BPVP_EFFECT_SHAKE)) result = bpvp_apply_shake(result, bus);
    if (bpvp_has(bus, BPVP_EFFECT_HEAT_HAZE)) result = bpvp_apply_heat_haze(result, bus);
    return result;
}

#endif
