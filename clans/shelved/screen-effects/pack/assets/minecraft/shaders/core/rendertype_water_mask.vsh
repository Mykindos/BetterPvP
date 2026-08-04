#version 330
#moj_import <betterpvp:bpvp_effects_vertex.glsl>
#moj_import <minecraft:globals.glsl>


#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;

void main() {
    vec4 bpvp_clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    bpvp_clip = bpvp_apply_vertex_effects(bpvp_clip, GameTime);
    gl_Position = bpvp_clip;
}