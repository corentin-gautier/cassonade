#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:texture_sampling.glsl>
#include <minecraft:oit.glsl>
#include <minecraft:terrainglobals.glsl>
#ifndef MULTIDRAW_TERRAIN
    #include <minecraft:chunksection.glsl>
#endif

uniform sampler2D Sampler0;
uniform sampler2D SkySampler;
uniform sampler2D SkyAboveSampler;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in float chunkVisibility;
layout(location = 5) in vec3 viewPosition;
layout(location = 6) in float skyBrightness;
layout(location = 7) in float skyLight;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif
    return apply_fog(color, sphericalVertexDistance, 0.0, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}

void main() {
    // The world ends on a true circle, inside the last chunks' corners and gaps (the disc in SkyRendererMixin has the same edge)
    if (length(viewPosition.xz) > FogRenderDistanceEnd - 16.0) {
        discard;
    }

    vec4 color = (UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, 1.0f / TextureSize) : sampleNearest(Sampler0, texCoord0, 1.0f / TextureSize)) * vertexColor;
    #ifndef OIT_ALPHA_ONLY
    color = mix(FogColor * vec4(1, 1, 1, color.a), color, chunkVisibility);
    #endif
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    // Translucent and lying flat: the sea, not a window, nor the soft edges of leaves in a cutout pass
    #ifdef ALPHA_CUTOUT
    bool translucentPass = ALPHA_CUTOUT < 0.5;
    #else
    bool translucentPass = true;
    #endif
    // The air between: far terrain takes the color of the very sky behind it, gone at the world's edge
    float edge = FogRenderDistanceEnd - 16.0;
    float air = smoothstep(0.7, 1.0, length(viewPosition.xz) / edge);
    if (translucentPass && color.a < 1.0 && abs(normalize(cross(dFdx(viewPosition), dFdy(viewPosition))).y) > 0.9) {
        // A look skimming the surface sinks so slowly that it leaves the last chunks before reaching the seabed, showing the sky behind the kelp: like a real sea, it is opaque
        float skimming = viewPosition.y < 0.0 ? 1.0 - smoothstep(0.08, 0.3, -viewPosition.y / length(viewPosition.xz)) : 0.0;
        // After the cutout, so holes stay holes: far water hides what lies under the sea
        color.a = mix(color.a, 1.0, max(skimming, smoothstep(48.0, 128.0, cylindricalVertexDistance)));
        // At dusk and dawn the sea loses its blue and darkens, and keeps little of it at night; full day stays as it is
        float night = 1.0 - smoothstep(0.25, 0.5, skyBrightness);
        float dusk = (1.0 - night) * (1.0 - smoothstep(0.75, 1.0, skyBrightness));
        color.rgb = mix(color.rgb, vec3(dot(color.rgb, vec3(0.3, 0.59, 0.11))), 0.4 * (dusk + night)) * (1.0 - 0.45 * dusk) * (1.0 - 0.35 * night);
        // From under water the sky only shows looking up, within 48 degrees of straight up (Snell's window): past it the surface is a mirror.
        // Only where the sky reaches this water, not in a flooded cave. Blurred, as waves would, so the sun is a glow and not a sharp square
        if (viewPosition.y > 0.0) {
            // Past the window the mirror shows the depths below: the water's own dim color, keeping a trace of the waves.
            // Never brighter than the surface itself, which in a dark flooded cave is darker than the water's color
            float cone = smoothstep(0.62, 0.72, viewPosition.y / length(viewPosition));
            color = vec4(mix(color.rgb, min(color.rgb, FogColor.rgb), 0.8 * (1.0 - cone)), mix(color.a, 1.0, 1.0 - cone));
            vec2 pixel = 12.0 / vec2(textureSize(SkyAboveSampler, 0));
            vec2 uv = gl_FragCoord.xy * pixel / 12.0;
            vec4 above = (texture(SkyAboveSampler, uv) + texture(SkyAboveSampler, uv + pixel) + texture(SkyAboveSampler, uv - pixel)
                    + texture(SkyAboveSampler, uv + vec2(pixel.x, -pixel.y)) + texture(SkyAboveSampler, uv + vec2(-pixel.x, pixel.y))) / 5.0;
            // The glow around the sun and moon shines through whole, where the rest of the sky only tints the waves
            float glare = smoothstep(0.5, 1.0, dot(above.rgb, vec3(0.3, 0.59, 0.11)));
            float window = mix(0.6, 1.0, glare) * above.a * cone * smoothstep(0.6, 1.0, skyLight);
            color = vec4(mix(color.rgb, above.rgb, window), mix(color.a, 1.0, window));
        }
        // No haze at the edge, the sea keeps its color up to a line softened by 8 pixels, wherever it is seen from
        float reach = length(viewPosition.xz);
        air = smoothstep(edge - 8.0 * fwidth(reach), edge, reach);
    }

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    vec4 sky = texture(SkySampler, gl_FragCoord.xy / vec2(textureSize(SkySampler, 0)));
    color.rgb = mix(color.rgb, sky.a > 0.0 ? sky.rgb : FogColor.rgb, air);
    fragColor = calculateFinalColor(color);
    #endif
}
