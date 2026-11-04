#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <minecraft:terrainglobals.glsl>
#ifndef MULTIDRAW_TERRAIN
    #include <minecraft:chunksection.glsl>
#endif

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
#ifdef MULTIDRAW_TERRAIN
layout(location = 4) in ivec3 ChunkPosition;
layout(location = 5) in float ChunkVisibility;
#endif

#ifndef OIT_ALPHA_ONLY
uniform sampler2D Sampler2;
#endif

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;
layout(location = 4) out float chunkVisibility;
layout(location = 5) out vec3 viewPosition;
layout(location = 6) out float skyBrightness;
layout(location = 7) out float skyLight;

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    viewPosition = pos;
    skyLight = UV2.y / 240.0;
    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    // Cassonade's colored light (ColoredLight): an alpha under 1, never seen on terrain otherwise, holds the hue
    // of the light in front of this vertex, its index in the low 5 bits and its saturation in the high 3
    vec4 color = Color;
    #ifndef OIT_ALPHA_ONLY
    if (Color.a < 254.5 / 255.0) {
        int code = int(Color.a * 255.0 + 0.5);
        float hue = float(code & 31) / 32.0;
        vec3 tint = mix(vec3(1.0), clamp(abs(mod(hue * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0), float(code >> 5) / 7.0);
        // As much as the blocks' light outshines the sky's right now: washed out by day, back at night
        const vec3 luma = vec3(0.3, 0.59, 0.11);
        float blockGlow = dot(sample_lightmap(Sampler2, ivec2(UV2.x, 0)).rgb, luma);
        float skyGlow = dot(sample_lightmap(Sampler2, ivec2(0, UV2.y)).rgb, luma);
        float share = clamp((blockGlow - skyGlow) / max(blockGlow, 0.001), 0.0, 1.0);
        color = vec4(Color.rgb * mix(vec3(1.0), tint, 0.65 * share), 1.0);
    }
    vertexColor = color * sample_lightmap(Sampler2, UV2);
    // What the open sky gives right now, whatever the light where this vertex stands
    skyBrightness = dot(sample_lightmap(Sampler2, ivec2(0, 240)).rgb, vec3(0.3, 0.59, 0.11));
    #else
    vertexColor = Color.a < 254.5 / 255.0 ? vec4(Color.rgb, 1.0) : Color;
    skyBrightness = 1.0;
    #endif
    texCoord0 = UV0;

    const float chunkFullyVisibleRange = 16.0;
    float dist = length(pos);
    chunkVisibility = mix(1.0, ChunkVisibility, clamp((dist - chunkFullyVisibleRange) / chunkFullyVisibleRange, 0.0, 1.0));
}
