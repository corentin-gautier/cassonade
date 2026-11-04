#version 330
#extension GL_ARB_separate_shader_objects : require

// Eyes open under water see blurred, more so the farther things are: one direction of a gaussian blur whose width
// follows the distance read from the depth buffer, and the screen's height, so it looks the same at any resolution.

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
    vec2 DepthSize;
};

layout(std140) uniform UnderwaterBlur {
    vec2 BlurDir;
    // In pixels of a 1080-pixel-high screen, at the farthest
    float Radius;
    // How much of it is left right before the eyes
    float NearShare;
    // Blocks at which it is full
    float FullDistance;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

// The game's near plane; its far one is far enough away to leave out
const float NEAR = 0.05;
const int TAPS = 6;

void main() {
    // The depth is NEAR / distance when reversed, 1 - NEAR / distance when not: either way past half only within a
    // tenth of a block of the eyes, or everywhere else, which tells one from the other
    float depth = texture(DepthSampler, texCoord).r;
    float fromFar = depth > 0.5 ? 1.0 - depth : depth;
    float distance = NEAR / max(fromFar, 1e-6);
    float radius = Radius * InSize.y / 1080.0 * mix(NearShare, 1.0, smoothstep(0.0, FullDistance, distance));
    vec2 step = BlurDir / InSize * radius / float(TAPS);
    vec4 sum = vec4(0.0);
    float weights = 0.0;
    for (int i = -TAPS; i <= TAPS; i++) {
        // A gaussian whose deviation is half the radius
        float weight = exp(-float(i * i) / 18.0);
        sum += texture(InSampler, texCoord + step * float(i)) * weight;
        weights += weight;
    }
    fragColor = sum / weights;
}
