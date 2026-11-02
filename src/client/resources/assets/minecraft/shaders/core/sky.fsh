#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, 0.0, FogSkyEnd, FogSkyEnd, FogSkyEnd);
    // The game's straight fade quickens up to the horizon's color and stops dead, which shows as a line across the sky:
    // this one slows down as it arrives
    fogValue = 1.0 - (1.0 - fogValue) * (1.0 - fogValue);
    // The disc closing the horizon (alpha 0, see SkyRendererMixin) is the fog's color all over, whatever its distance
    if (ColorModulator.a == 0.0) {
        fogValue = 1.0;
    }
    fragColor = vec4(mix(ColorModulator.rgb, FogColor.rgb, fogValue * FogColor.a), 1.0);
}
