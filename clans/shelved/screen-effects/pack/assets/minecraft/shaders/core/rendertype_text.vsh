#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <betterpvp:bpvp_effects_vertex.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

// ============================================================================
// BetterPvP HUD screen-anchor
// ----------------------------------------------------------------------------
// Pins the clan/info HUD (the boss-bar overlay painted by FontCanvas) flush to
// the LEFT edge of the screen, and the world-event HUD flush to the RIGHT edge,
// independent of GUI scale and monitor resolution.
//
// Why a shader is needed: the boss-bar title is centred by vanilla at the
// screen's horizontal middle, and the server can never know the client's
// scaled screen width (it depends on resolution AND the GUI-scale option), so
// no fixed `space(-N)` offset can reliably reach the left edge. The GPU, on the
// other hand, works in clip space where the screen edge is ALWAYS at x = +-w
// regardless of scale -- so a single constant shift re-anchors the HUD exactly.
//
// Tagging: this core shader runs on ALL text (chat, scoreboards, name tags,
// tooltips, signs...), so the HUD must be distinguishable. Adventure's
// TextColor cannot carry an alpha flag (alpha is stripped to opaque), so the
// flag is hidden in the LOW 3 BITS of each colour channel instead -- a signature
// the eye can't see (<=7/255 per channel) that rides along with ANY colour. That
// matters for the player-head pixels, which keep their own skin colours; an exact
// sentinel could never match them, but the low-bit signature can. The Java side
// stamps this signature (see HudAnchor / PlayerHeadProvider `mark(...)`).
//
// Two independent signatures, one per edge (kept in lockstep with HudAnchor.java):
//   LEFT  = (5,2,5) -> colour 0xFDFAFD   (clan/info HUD, left edge)
//   RIGHT = (5,2,6) -> colour 0xFDFAFE   (world-event HUD, right edge)
//
// Collisions: ~1/512 of unrelated GUI glyphs share a signature by chance, so
// re-anchoring is additionally gated to the TOP STRIP where the boss bar renders.
// Chat (bottom) and the scoreboard (right/lower) are excluded outright; only a
// glyph that is both signed AND near the top (e.g. a tooltip) can collide.
//
// Contract each HUD component must satisfy:
//   1. Every glyph to re-anchor carries its edge's signature (LEFT or RIGHT).
//   2. The component has ~zero net horizontal advance, so vanilla centres its
//      origin on the screen middle; the shader then maps that origin onto the
//      target edge. LEFT builds the layout rightward FROM the origin; RIGHT
//      builds it leftward TO the origin (drop any big `space(-N)` walk -- the
//      shader replaces it).
//   3. Vertical placement is unchanged: the boss bar already sits a fixed
//      GUI-pixel offset below the top edge, so the existing down_N fonts keep
//      controlling the vertical position. Only the X axis is re-anchored here.
// ============================================================================

// Inset from the anchored edge, in GUI pixels (scales with GUI scale like the
// rest of the HUD). 0.0 = flush against the edge. Symmetric on both edges.
const float BPVP_LEFT_INSET_PX  = 2.0;
const float BPVP_RIGHT_INSET_PX = 2.0;
// How far down from the top edge (GUI pixels) the re-anchor still applies. Must
// be tall enough to contain the HUD's boss bar even when other boss bars stack
// above it; too tight clips the HUD, too loose lets more tooltip collisions in.
const float BPVP_TOP_STRIP_PX  = 120.0;

// A glyph is flagged for an edge when the low 3 bits of each channel match that
// edge's signature. Round the 0..1 channel back to a 0..255 int and test the low
// bits. LEFT = (5,2,5); RIGHT differs only in blue's low bit (6) -> 0xFDFAFE.
bool bpvp_isMarkedLeft(vec4 color) {
    int r = int(color.r * 255.0 + 0.5);
    int g = int(color.g * 255.0 + 0.5);
    int b = int(color.b * 255.0 + 0.5);
    return (r & 7) == 5 && (g & 7) == 2 && (b & 7) == 5;
}

bool bpvp_isMarkedRight(vec4 color) {
    int r = int(color.r * 255.0 + 0.5);
    int g = int(color.g * 255.0 + 0.5);
    int b = int(color.b * 255.0 + 0.5);
    return (r & 7) == 5 && (g & 7) == 2 && (b & 7) == 6;
}

// Orthographic (GUI / HUD) projection has ProjMat[3][3] == 1.0; the perspective
// world projection has it == 0.0. Gating on this leaves all world-space text
// (name tags, holograms, signs) completely untouched.
bool bpvp_isGuiProjection() {
    return ProjMat[3][3] > 0.5;
}

void main() {
    vec4 pos = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // GUI pixels from the top edge: top edge is clip y == +w, and ProjMat[1][1]
    // == -2 / guiHeight, so guiHeight = -2 / ProjMat[1][1].
    float guiHeight = -2.0 / ProjMat[1][1];
    float guiY = (1.0 - pos.y / pos.w) * 0.5 * guiHeight;

    // Screen effects apply to world text only -- name tags and holograms belong
    // to the scene and should bank with it, whereas rolling the HUD would tip
    // the chat and the scoreboard along with the horizon.
    if (!bpvp_isGuiProjection()) {
        pos = bpvp_apply_vertex_effects(pos, GameTime);
    }

    if (bpvp_isGuiProjection() && guiY < BPVP_TOP_STRIP_PX) {
        // Screen centre is clip x == 0; the edges are clip x == -+w. The HUD's
        // origin is centred (see contract), so shifting every marked glyph by one
        // half-width drops that origin onto the target edge while keeping the
        // layout's internal spacing intact. ProjMat[0][0] == 2 / guiWidth converts
        // the pixel inset into clip space so it stays a fixed margin.
        if (bpvp_isMarkedLeft(Color)) {
            pos.x -= pos.w;
            pos.x += BPVP_LEFT_INSET_PX * ProjMat[0][0] * pos.w;
        } else if (bpvp_isMarkedRight(Color)) {
            pos.x += pos.w;
            pos.x -= BPVP_RIGHT_INSET_PX * ProjMat[0][0] * pos.w;
        }
    }

    gl_Position = pos;

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
}
