#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;

layout(location = 0) out vec4 fragColor;

// Settings
// How far up the sky the horizon's haze reaches: it is gone where the game's own fog would end this many times nearer.
// Higher climbs higher (20: about 60 degrees up)
const float HAZE_REACH = 20.0;
// How slowly that haze dies out as it climbs: the power of what is left of it (1: straight, higher: thinner up high)
const float HAZE_FALLOFF = 3.0;

void main() {
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, 0.0, FogSkyEnd, FogSkyEnd, FogSkyEnd);
    // The game's straight fade quickens up to the horizon's color and stops dead, which shows as a line across the sky:
    // this one slows down as it arrives
    fogValue = 1.0 - (1.0 - fogValue) * (1.0 - fogValue);
    // The sky disc lies flat 16 blocks overhead, so its distance only grows close to the horizon: the game's fade fits in
    // the last few degrees above it and showed as a pale band. This haze climbs far up the sky and dies out slowly. The
    // game's fade still wins where the mist or a storm brings the sky's fog end close, so that they hide the whole sky
    float haze = 1.0 - clamp(FogSkyEnd / (sphericalVertexDistance * HAZE_REACH), 0.0, 1.0);
    fogValue = max(fogValue, pow(haze, HAZE_FALLOFF));
    // The disc closing the horizon (alpha 0, see SkyRendererMixin) is the fog's color all over, whatever its distance
    if (ColorModulator.a == 0.0) {
        fogValue = 1.0;
    }
    fragColor = vec4(mix(ColorModulator.rgb, FogColor.rgb, fogValue * FogColor.a), 1.0);
}
