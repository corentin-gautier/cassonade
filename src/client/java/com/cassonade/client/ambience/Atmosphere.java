package com.cassonade.client.ambience;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributes;

import com.cassonade.client.dev.DevRendering;

/**
 * The colors of a real sky over the Overworld: a deep blue overhead, paling towards the horizon, and distant land fading into
 * the blue of the air rather than into white. Biomes keep their own sky, as these only tint what the game asks for. The morning
 * mist stays white (MorningMist), as rain stays grey and a sunset orange.
 */
public final class Atmosphere {
	private static final float SKY_RED = 0.62F;
	private static final float SKY_GREEN = 0.78F;
	private static final float SKY_BLUE = 0.92F;
	/** How far the fog goes from the game's nearly white towards its sky color. */
	private static final float FOG_BLUE = 0.55F;

	private Atmosphere() {
	}

	public static Vector3fc sky(ClientLevel level, Vector3fc color, float partialTicks) {
		if (!Ambience.overworld(level) || DevRendering.vanilla()) {
			return color;
		}
		Vector3f deep = new Vector3f(color.x() * SKY_RED, color.y() * SKY_GREEN, color.z() * SKY_BLUE);
		return ARGB.srgbLerp(level.getRainLevel(partialTicks), deep, new Vector3f(color));
	}

	public static Vector3fc fog(ClientLevel level, Camera camera, Vector3fc color, float partialTicks) {
		if (!Ambience.overworld(level) || DevRendering.vanilla()) {
			return color;
		}
		float sunset = camera.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTicks).w();
		float blue = FOG_BLUE * (1 - level.getRainLevel(partialTicks)) * (1 - sunset) * (1 - MorningMist.density());
		return ARGB.srgbLerp(blue, color, new Vector3f(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_COLOR, partialTicks)));
	}
}
