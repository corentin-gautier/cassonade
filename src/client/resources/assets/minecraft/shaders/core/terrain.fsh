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

// Settings
// Far land: how much of the sky behind it takes over at the render distance (1: it vanishes, lower: it stays visible)
const float FARTHEST_HAZE = 0.9;
// The sea's edge: how many blocks inside the render distance it ends (the same in SkyRendererMixin, for the disc hiding
// the sun behind it), and how many pixels soften it
const float SEA_EDGE_INSET = 16.0;
const float SEA_EDGE_SOFTNESS = 8.0;
// How many pixels above the sea's edge it reads the sky it melts into (past the disc hiding the sun, behind the edge)
const float SEA_EDGE_SKY_ABOVE = 9.0;
// Far water turns opaque between these distances, in blocks, and when looked at this skimming (slope of the look)
const float SEA_OPAQUE_FROM = 48.0;
const float SEA_OPAQUE_TO = 128.0;
const float SEA_SKIMMING_FROM = 0.08;
const float SEA_SKIMMING_TO = 0.3;
// The sea at dusk and dawn, then at night: how much of its color it loses and how much darker it gets
const float SEA_DUSK_GREY = 0.4;
const float SEA_DUSK_DARK = 0.45;
const float SEA_NIGHT_DARK = 0.85;
// Under water: how dark the mirror past Snell's window is, and how much of the sky shows through the window
const float MIRROR_DARK = 0.8;
const float WINDOW_SKY = 0.6;
// Under water: how blurred the sky seen through the surface is, the radius in pixels of a soft round blur
const float WINDOW_BLUR = 16.0;
// Under water at night: how much of its own color the surface keeps (0: black), the sky through the window aside
const float SURFACE_NIGHT = 0.12;

void main() {
    // Land goes as far as the game draws it, melting into the sky; the sea alone stops SEA_EDGE_INSET blocks before, on a true circle inside
    // the last chunks' corners and gaps (the disc in SkyRendererMixin has the same edge)
    if (length(viewPosition.xz) > FogRenderDistanceEnd) {
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
    // The air between: far land takes the color of the very sky behind it (the game's fog color, not quite the sky's, drew it
    // out against the sky), over the game's own render distance fog and as far as the game draws it. Never whole: fully the
    // sky's color, the farthest trees and hills vanished, where the game keeps them as shapes the color of the haze
    float edge = FogRenderDistanceEnd - SEA_EDGE_INSET;
    float air = FARTHEST_HAZE * linear_fog_value(length(viewPosition.xz), FogRenderDistanceStart, FogRenderDistanceEnd);
    // Where to read the sky behind, in pixels above this one
    float skyAbove = 0.0;
    if (translucentPass && color.a < 1.0 && abs(normalize(cross(dFdx(viewPosition), dFdy(viewPosition))).y) > 0.9) {
        // A look skimming the surface sinks so slowly that it leaves the last chunks before reaching the seabed, showing the sky behind the kelp: like a real sea, it is opaque
        float skimming = viewPosition.y < 0.0 ? 1.0 - smoothstep(SEA_SKIMMING_FROM, SEA_SKIMMING_TO, -viewPosition.y / length(viewPosition.xz)) : 0.0;
        // After the cutout, so holes stay holes: far water hides what lies under the sea
        color.a = mix(color.a, 1.0, max(skimming, smoothstep(SEA_OPAQUE_FROM, SEA_OPAQUE_TO, cylindricalVertexDistance)));
        // At dusk and dawn the sea loses its blue and darkens, and keeps little of it at night; full day stays as it is
        float night = 1.0 - smoothstep(0.25, 0.5, skyBrightness);
        float dusk = (1.0 - night) * (1.0 - smoothstep(0.65, 0.8, skyBrightness));
        color.rgb = mix(color.rgb, vec3(dot(color.rgb, vec3(0.3, 0.59, 0.11))), SEA_DUSK_GREY * (dusk + night)) * (1.0 - SEA_DUSK_DARK * dusk) * (1.0 - SEA_NIGHT_DARK * night);
        // From under water the sky only shows looking up, within 48 degrees of straight up (Snell's window): past it the surface is a mirror.
        // Only where the sky reaches this water, not in a flooded cave. Blurred, as waves would, so the sun is a glow and not a sharp square
        if (viewPosition.y > 0.0) {
            // Past the window the mirror shows the depths below: the water's own dim color, keeping a trace of the waves.
            // Never brighter than the surface itself, which in a dark flooded cave is darker than the water's color
            float cone = smoothstep(0.62, 0.72, viewPosition.y / length(viewPosition));
            color = vec4(mix(color.rgb, min(color.rgb, FogColor.rgb), MIRROR_DARK * (1.0 - cone)), mix(color.a, 1.0, 1.0 - cone));
            // At night the surface seen from below is all but black, as in a real sea, yet still there to find one's way
            color.rgb *= mix(1.0, SURFACE_NIGHT, night);
            // A round blur, the center and three rings of 8, each turned from the last so that none lines up: shifted copies along
            // the diagonals drew stars as dashes and the moon twice
            vec2 texel = 1.0 / vec2(textureSize(SkyAboveSampler, 0));
            vec2 uv = gl_FragCoord.xy * texel;
            vec4 above = texture(SkyAboveSampler, uv);
            for (int ring = 1; ring <= 3; ring++) {
                float radius = WINDOW_BLUR * float(ring) / 3.0;
                for (int i = 0; i < 8; i++) {
                    float angle = (float(i) + 0.33 * float(ring)) * 0.785398;
                    above += texture(SkyAboveSampler, uv + vec2(cos(angle), sin(angle)) * radius * texel);
                }
            }
            above /= 25.0;
            // The glow around the sun and moon shines through whole, where the rest of the sky only tints the waves
            float glare = smoothstep(0.5, 1.0, dot(above.rgb, vec3(0.3, 0.59, 0.11)));
            float window = mix(WINDOW_SKY, 1.0, glare) * above.a * cone * smoothstep(0.6, 1.0, skyLight);
            color = vec4(mix(color.rgb, above.rgb, window), mix(color.a, 1.0, window));
        }
        // No haze at the edge, the sea keeps its color up to a line softened by a few pixels, wherever it is seen from, and ends there
        float reach = length(viewPosition.xz);
        if (reach > edge) {
            discard;
        }
        air = smoothstep(edge - SEA_EDGE_SOFTNESS * fwidth(reach), edge, reach);
        // Right behind the sea's edge lies the disc closing the horizon (SkyRendererMixin), all of the fog's color: fading into it
        // drew a line of that color under the setting sun. The sky just above the edge is what the edge melts into
        skyAbove = SEA_EDGE_SKY_ABOVE;
    }

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    vec4 sky = texture(SkySampler, (gl_FragCoord.xy + vec2(0.0, skyAbove)) / vec2(textureSize(SkySampler, 0)));
    color.rgb = mix(color.rgb, sky.a > 0.0 ? sky.rgb : FogColor.rgb, air);
    fragColor = calculateFinalColor(color);
    #endif
}
