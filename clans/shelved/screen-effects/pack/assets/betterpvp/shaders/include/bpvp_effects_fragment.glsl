#ifndef BPVP_EFFECTS_FRAGMENT
#define BPVP_EFFECTS_FRAGMENT

#moj_import <betterpvp:bpvp_bus.glsl>

// ============================================================================
// BetterPvP screen effects -- fragment half
// ----------------------------------------------------------------------------
// Effects that recolour rather than displace: full-scene grades with haze,
// weather particles and vignetting layered on top. Each takes the shaded colour
// of a fragment and returns a graded one, so a core shader opts in with a single
// line before writing fragColor.
//
// These run per fragment on every drawn surface, so they are the expensive half.
// Each preset early-outs on its bit, and the whole entrypoint early-outs when
// the bus is idle -- which is the common case for nearly every player.
//
// See bpvp_bus.glsl for how the server reaches these at all.
// ============================================================================

float bpvp_hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float bpvp_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = bpvp_hash12(i);
    float b = bpvp_hash12(i + vec2(1.0, 0.0));
    float c = bpvp_hash12(i + vec2(0.0, 1.0));
    float d = bpvp_hash12(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float bpvp_luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Radial falloff, 0 at the centre and 1 at the corners.
float bpvp_vignette(vec2 uv) {
    vec2 d = uv - 0.5;
    return smoothstep(0.12, 0.70, dot(d, d));
}

// ----------------------------------------------------------------------------
// WINTER -- a blizzard. Cold grade, whiteout haze that thickens with distance,
// and three parallax layers of falling snow drawn in screen space.
// ----------------------------------------------------------------------------

vec3 bpvp_grade_winter(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    c = (c - 0.5) * 1.18 + 0.56;
    c *= vec3(0.82, 0.93, 1.28);

    float l = bpvp_luminance(c);
    c += smoothstep(0.62, 0.10, l) * vec3(-0.04, -0.01, 0.10);
    c = mix(c, c * vec3(0.96, 0.98, 1.02), smoothstep(0.60, 0.95, l));
    c = mix(vec3(l), c, 0.68);

    return clamp(c, 0.0, 1.0);
}

vec3 bpvp_cold_haze(vec3 c, float dist01) {
    float haze = smoothstep(0.02, 1.0, dist01);
    return mix(c, vec3(0.92, 0.95, 1.00), clamp(haze * haze * 0.88, 0.0, 1.0));
}

// A single flake: a soft circle squashed vertically, so it reads as motion blur
// on something falling rather than as a dot.
float bpvp_snow_dot(vec2 f, vec2 centre, float radius) {
    vec2 d = f - centre;
    d.y *= 0.35;
    return smoothstep(radius, radius * 0.20, length(d));
}

// One parallax layer. The screen is cut into cells, each cell either holds a
// flake or does not, and the neighbours are sampled so a flake straddling a cell
// boundary is not clipped in half.
float bpvp_snow_layer(vec2 uv, float t, float scale, float fallSpeed, float wind, float density) {
    vec2 p = uv * scale;
    p.y += t * fallSpeed;
    p.x += (bpvp_noise(vec2(p.y * 0.12, t * 0.08)) - 0.5) * 2.0 * wind;

    vec2 baseCell = floor(p);
    vec2 f = fract(p);
    float best = 0.0;

    for (int oy = -1; oy <= 1; oy++) {
        for (int ox = -1; ox <= 1; ox++) {
            vec2 cell = baseCell + vec2(float(ox), float(oy));
            float present = smoothstep(1.0 - density - 0.03, 1.0 - density + 0.03, bpvp_hash12(cell));
            vec2 centre = vec2(bpvp_hash12(cell + 17.0), bpvp_hash12(cell + 29.0));
            float radius = mix(0.05, 0.18, bpvp_hash12(cell + 91.0));

            best = max(best, bpvp_snow_dot(f - vec2(float(ox), float(oy)), centre, radius) * present);
        }
    }

    return clamp(best, 0.0, 1.0);
}

float bpvp_whiteout(vec2 uv, float t, float dist01) {
    float veil = smoothstep(0.08, 1.0, dist01) * 0.78;
    veil += (bpvp_noise(uv * 7.5 + vec2(t * 0.22, -t * 0.16)) - 0.5) * 0.16;

    vec2 d = uv - 0.5;
    veil += smoothstep(0.06, 0.70, dot(d, d)) * 0.28;
    veil += 0.08;

    return clamp(veil, 0.0, 1.0);
}

vec4 bpvp_winter(vec4 color, float gameTime01, vec2 uv, float viewDist) {
    float t = gameTime01 * 6000.0;
    float dist01 = clamp(viewDist / 160.0, 0.0, 1.0);

    vec3 c = bpvp_cold_haze(bpvp_grade_winter(color.rgb), dist01);

    float snow = (bpvp_snow_layer(uv, t, 140.0, 0.65, 1.20, 0.16) * 0.55
                + bpvp_snow_layer(uv, t, 80.0, 0.45, 1.40, 0.14) * 0.75
                + bpvp_snow_layer(uv, t, 40.0, 0.25, 1.60, 0.12)) * 0.85;

    c = mix(c, vec3(0.92, 0.96, 1.00), clamp(snow, 0.0, 1.0));
    c = mix(c, vec3(0.86, 0.90, 1.00), bpvp_whiteout(uv, t, dist01));

    return vec4(clamp(c, 0.0, 1.0), color.a);
}

// ----------------------------------------------------------------------------
// DESERT -- baking heat. Warm grade, a haze that washes out anything more than a
// few blocks away, and a shimmer that crawls across the image. Pairs with the
// HEAT_HAZE vertex effect, which supplies the geometric wobble.
// ----------------------------------------------------------------------------

vec3 bpvp_grade_desert(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    c = (c - 0.5) * 1.10 + 0.54;
    c *= vec3(1.10, 1.05, 0.92);

    float l = bpvp_luminance(c);
    c += smoothstep(0.55, 0.95, l) * vec3(0.06, 0.03, -0.02);
    c += smoothstep(0.50, 0.10, l) * vec3(-0.01, 0.00, 0.03);
    c = mix(vec3(l), c, 0.92);

    return clamp(c, 0.0, 1.0);
}

// The distance at which the haze takes hold is jittered by noise, so the
// boundary crawls instead of sitting as a fixed ring around the player.
vec3 bpvp_heat_wash(vec3 c, float viewDist, vec2 uv, float t) {
    float jitter = (bpvp_noise(uv * 6.0 + vec2(t * 0.00012, -t * 0.00008)) - 0.5) * 4.0;
    float haze = smoothstep(max(0.0, 1.5 + jitter), 5.0, viewDist);
    return mix(c, vec3(1.00, 0.92, 0.78), clamp(haze * haze * 0.6, 0.0, 1.0));
}

float bpvp_shimmer_mask(vec2 uv, float t, float dist01) {
    float ts = t * 0.25;
    float n = bpvp_noise(uv * 10.0 + vec2(ts * 0.40, -ts * 0.25)) * 0.65
            + bpvp_noise(uv * 22.0 + vec2(-ts * 0.70, ts * 0.50)) * 0.35;
    n = pow(smoothstep(0.52, 0.82, n), 1.6);

    // Strongest far away and low on the screen, which is where real heat haze
    // pools -- over the ground, not over your head.
    float strength = 0.35 + 0.45 * smoothstep(0.10, 1.0, dist01) + 0.20 * smoothstep(0.20, 0.85, 1.0 - uv.y);
    return clamp(n * strength, 0.0, 1.0);
}

vec4 bpvp_desert(vec4 color, float gameTime01, vec2 uv, float viewDist) {
    float t = gameTime01 * 14000.0;
    float dist01 = max(clamp(viewDist * 0.02, 0.0, 1.0), clamp(viewDist * 2.0, 0.0, 1.0));

    vec3 c = bpvp_heat_wash(bpvp_grade_desert(color.rgb), viewDist, uv, t);

    float m = bpvp_shimmer_mask(uv, t, dist01);
    c = mix(c, vec3(1.00, 0.93, 0.80), m * 0.18);
    c.r += m * 0.028;
    c.b -= m * 0.022;

    return vec4(clamp(c, 0.0, 1.0), color.a);
}

// ----------------------------------------------------------------------------
// CAVE -- damp underground. Teal shadows against warm highlights, a haze that
// closes in within ten blocks, and a bloom around anything bright enough to be
// a torch.
// ----------------------------------------------------------------------------

vec3 bpvp_grade_cave(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    c = (c - 0.5) * 1.12 + 0.50;
    c *= vec3(1.02, 1.00, 0.96);

    float l = bpvp_luminance(c);
    float shadow = smoothstep(0.45, 0.08, l);
    c += shadow * (vec3(-0.03) + vec3(0.00, 0.03, 0.05));
    c -= shadow * vec3(0.03);
    c += smoothstep(0.55, 0.95, l) * vec3(0.14, 0.08, -0.06);
    c = mix(vec3(l), c, 0.85);

    return clamp(c, 0.0, 1.0);
}

vec3 bpvp_cave_haze(vec3 c, float viewDist, vec2 uv, float t) {
    float jitter = (bpvp_noise(uv * 6.0 + vec2(t * 0.00012, -t * 0.00008)) - 0.5) * 4.0;
    float haze = smoothstep(max(0.0, 3.0 + jitter), 10.0, viewDist);
    return mix(c, vec3(0.22, 0.26, 0.32), clamp(haze * haze * 0.6, 0.0, 1.0));
}

// There is no light-source list in a core shader, so "is this a torch" is
// guessed from the fragment itself: bright, and close enough to matter.
float bpvp_torch_strength(vec3 lit, float viewDist) {
    return clamp(smoothstep(0.55, 0.90, bpvp_luminance(lit)) * (1.0 - smoothstep(6.0, 24.0, viewDist)), 0.0, 1.0);
}

vec3 bpvp_torch_bloom(vec3 c, float torch, vec2 uv) {
    if (torch <= 0.001) {
        return c;
    }

    vec2 d = uv - 0.5;
    float bloom = smoothstep(0.25, 0.02, dot(d, d)) * torch;

    vec3 glow = vec3(1.00, 0.72, 0.38);
    return clamp(mix(c, glow, bloom * 0.35) + glow * bloom * 0.08, 0.0, 1.0);
}

vec3 bpvp_torch_air(vec3 c, float torch, vec2 uv, float dist01) {
    float wide = 1.0 - smoothstep(0.0, 0.85, length(uv - 0.5));
    float height = smoothstep(0.15, 0.85, 1.0 - uv.y);
    float far = smoothstep(0.25, 1.0, dist01);

    float veil = wide * (0.35 + 0.65 * height) * (0.35 + 0.65 * far) * (0.90 + 0.10 * bpvp_noise(uv * 7.0));
    float amount = torch * veil * 0.35;

    vec3 haze = vec3(1.00, 0.78, 0.45);
    return clamp(mix(c, haze, amount) + haze * amount * 0.10, 0.0, 1.0);
}

vec4 bpvp_cave(vec4 color, float gameTime01, vec2 uv, float viewDist) {
    float t = gameTime01 * 3000.0;
    float dist01 = clamp(viewDist / 160.0, 0.0, 1.0);

    vec3 c = bpvp_cave_haze(bpvp_grade_cave(color.rgb), viewDist, uv, t);

    float torch = bpvp_torch_strength(color.rgb, viewDist);
    c = bpvp_torch_air(bpvp_torch_bloom(c, torch, uv), torch, uv, dist01);

    c *= (1.0 - bpvp_vignette(uv) * 0.28);
    c += (bpvp_noise(uv * 880.0 + vec2(t * 0.15, -t * 0.10)) - 0.5) * 0.45 * 0.015;

    return vec4(clamp(c, 0.0, 1.0), color.a);
}

// ----------------------------------------------------------------------------

vec4 bpvp_apply_fragment_effects(vec4 color, float gameTime01, vec2 screenUV, float viewDist) {
    BpvpBus bus = bpvp_decode(gameTime01);
    if (bus.mask == 0) {
        return color;
    }

    vec4 result = color;
    if (bpvp_has(bus, BPVP_EFFECT_WINTER)) result = bpvp_winter(result, bus.time, screenUV, viewDist);
    if (bpvp_has(bus, BPVP_EFFECT_DESERT)) result = bpvp_desert(result, bus.time, screenUV, viewDist);
    if (bpvp_has(bus, BPVP_EFFECT_CAVE)) result = bpvp_cave(result, bus.time, screenUV, viewDist);
    return result;
}

#endif
