package com.cassonade.client.ambience;

import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.weather.SandDrifts;

/**
 * When it rains, a blizzard where it snows and a sandstorm in the deserts and badlands (SandDrifts), where the game's weather shows nothing:
 * the fog drawn close, white or the color of the sand, hiding the sky, with snow or sand blown past on the wind. Thicker in a
 * thunderstorm, gone indoors (MorningMist's outdoors), and blending from one to the other across a biome's edge.
 * Drawn on the fog the way the morning mist is (AtmosphericFogEnvironmentMixin, SkyRendererMixin). The wind is heard all around,
 * swelling with the gusts the grains follow, a blizzard's calm in the rain and a gale in a thunderstorm; indoors it goes on
 * behind the walls, muffled like the rain (Shelter), and dies out underground.
 */
public final class Storm {
	/** The sandstorm's wind, a rumble in the rain, a gale in a thunderstorm: one fades into the other as the thunder comes. */
	public static final Identifier WIND = Cassonade.id("wind");
	public static final Identifier WIND_THUNDER = Cassonade.id("wind_thunder");
	/** A blizzard's, calm in the rain, a gale in a thunderstorm, the same way. */
	public static final Identifier BLIZZARD = Cassonade.id("blizzard");
	public static final Identifier BLIZZARD_THUNDER = Cassonade.id("blizzard_thunder");
	private static final float WIND_VOLUME = 0.25F;
	/** The sandstorm's is a deep rumble, which sounds quieter than the blizzard's at the same level. */
	private static final float SAND_WIND_VOLUME = 0.45F;
	/** Volume per tick: as slow as the storm coming in, so the two arrive together. */
	private static final float WIND_EASE = 0.003F;
	/** How much of its volume the wind gains or loses with a gust, on top of the recording's own. */
	private static final float WIND_GUSTS = 0.15F;
	/** Sky light from which the wind is heard in full: in a house with windows it still is, behind walls; deeper in, less. */
	private static final int WIND_SKY = 8;
	private static final AmbientLoop WIND_LOOP = new AmbientLoop(WIND, WIND_EASE);
	private static final AmbientLoop WIND_THUNDER_LOOP = new AmbientLoop(WIND_THUNDER, WIND_EASE);
	private static final AmbientLoop BLIZZARD_LOOP = new AmbientLoop(BLIZZARD, WIND_EASE);
	private static final AmbientLoop BLIZZARD_THUNDER_LOOP = new AmbientLoop(BLIZZARD_THUNDER, WIND_EASE);
	private static final Identifier SPRITE = Identifier.withDefaultNamespace("generic_0");
	/** Fog end at the storm's thickest, in a thunderstorm: a plain rain's storm is some way short of it (eased geometrically). */
	private static final float SNOW_END = 20;
	private static final float SAND_END = 28;
	private static final float SKY_END = 16;
	private static final float CLOUD_END = 48;
	/** A plain rain's share of a thunderstorm's thickness. */
	private static final float RAIN_SHARE = 0.7F;
	private static final float EASE = 0.01F;
	private static final float COLOR_EASE = 0.02F;
	private static final float PALE = 0.8F;
	/** The fog's brightest channel from which the storm has its full color: the rain darkens the game's fog, the night more. */
	private static final float DAYLIGHT = 0.35F;
	private static final Vector3f SNOW = new Vector3f(0.82F, 0.85F, 0.9F);
	private static final Vector3f SAND = new Vector3f(0.9F, 0.74F, 0.48F);
	private static final Vector3f RED_SAND = new Vector3f(0.8F, 0.48F, 0.28F);
	private static final float[] SNOW_GRAIN = {0.74F, 0.77F, 0.84F};
	private static final float[] SAND_GRAIN = {0.7F, 0.58F, 0.38F};
	private static final float[] RED_SAND_GRAIN = {0.78F, 0.46F, 0.26F};
	private static final float RANGE = 10;
	private static final int SNOW_GRAINS = 400;
	private static final int SAND_GRAINS = 420;
	/** Sand is carried low, bouncing over the ground; snow swirls at any height. */
	private static final int SAND_HEIGHT = 5;
	private static final float SPEED = 0.5F;
	private static final RandomSource RANDOM = RandomSource.create();
	private static float density;
	/** The storm around, indoors too: what the wind is heard at. */
	private static float strength;
	private static final Vector3f color = new Vector3f(SNOW);
	private static float[] grain = SNOW_GRAIN;

	private Storm() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			density = 0;
			strength = 0;
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		BlockPos pos = minecraft.player.blockPosition();
		Holder<Biome> biome = level.getBiome(pos);
		float target = 0;
		strength = 0;
		boolean snow = false;
		if (Ambience.overworld(level) && level.isRaining() && Settings.on(Setting.STORMS)) {
			Vector3f kind = kind(level, biome, pos);
			if (kind != null) {
				snow = kind == SNOW;
				strength = Mth.lerp(level.getThunderLevel(1), RAIN_SHARE, 1) * level.getRainLevel(1);
				target = strength * MorningMist.outdoors(level, pos);
				// Already in a storm, crossing into another: the colors blend; from clear air, the storm comes in its own
				if (density < 0.01F) {
					color.set(kind);
				} else {
					color.lerp(kind, COLOR_EASE);
				}
				grain = kind == SNOW ? SNOW_GRAIN : kind == SAND ? SAND_GRAIN : RED_SAND_GRAIN;
			}
		}
		density = Mth.approach(density, target, EASE);
		float sky = Math.min(level.getBrightness(LightLayer.SKY, BlockPos.containing(minecraft.player.getEyePosition())), WIND_SKY) / (float) WIND_SKY;
		float wind = WIND_VOLUME * strength * sky * (1 + WIND_GUSTS * gust(level));
		// At constant power, so the storm is no quieter halfway from its calm to its gale
		float thunder = level.getThunderLevel(1);
		float sand = snow ? 0 : wind * SAND_WIND_VOLUME / WIND_VOLUME;
		WIND_LOOP.follow(minecraft, sand * Mth.sqrt(1 - thunder));
		WIND_THUNDER_LOOP.follow(minecraft, sand * Mth.sqrt(thunder));
		BLIZZARD_LOOP.follow(minecraft, snow ? wind * Mth.sqrt(1 - thunder) : 0);
		BLIZZARD_THUNDER_LOOP.follow(minecraft, snow ? wind * Mth.sqrt(thunder) : 0);
		if (density > 0 && minecraft.options.particles().get() != ParticleStatus.MINIMAL) {
			blow(minecraft, level);
		}
	}

	/** Where it snows rather than rains, a blizzard; in a desert or the badlands, where it never rains, a sandstorm. */
	private static Vector3f kind(ClientLevel level, Holder<Biome> biome, BlockPos pos) {
		if (biome.value().hasPrecipitation()) {
			return biome.value().coldEnoughToSnow(pos, level.getSeaLevel()) ? SNOW : null;
		}
		if (!SandDrifts.sandstorm(biome)) {
			return null;
		}
		return biome.is(BiomeTags.IS_BADLANDS) ? RED_SAND : SAND;
	}

	/** Grains around the player, under the open sky, all going the same way, which changes from one day to the next. */
	private static void blow(Minecraft minecraft, ClientLevel level) {
		boolean sand = grain != SNOW_GRAIN;
		int count = Math.round((sand ? SAND_GRAINS : SNOW_GRAINS) * density);
		if (minecraft.options.particles().get() == ParticleStatus.DECREASED) {
			count /= 2;
		}
		// The same wind the sand piles up behind on the server
		float bearing = SandDrifts.windBearing(level.getDefaultClockTime());
		Vec3 wind = new Vec3(Mth.cos(bearing), 0, Mth.sin(bearing));
		float speed = SPEED * (0.8F + 0.3F * gust(level));
		Vec3 eye = minecraft.player.getEyePosition();
		TextureAtlasSprite sprite = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(SPRITE);
		for (int i = 0; i < count; i++) {
			// Mostly from upwind, so they cross the view rather than leave it
			double x = eye.x + (RANDOM.nextFloat() * 2 - 1) * RANGE - wind.x * RANGE / 2;
			double z = eye.z + (RANDOM.nextFloat() * 2 - 1) * RANGE - wind.z * RANGE / 2;
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
			double y = sand ? ground + RANDOM.nextFloat() * RANDOM.nextFloat() * SAND_HEIGHT : eye.y + (RANDOM.nextFloat() * 2 - 1) * 8;
			if (y < ground || !level.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
				continue;
			}
			minecraft.particleEngine.add(new Grain(level, x, y, z, sprite, grain, wind.scale(speed * Mth.randomBetween(RANDOM, 0.8F, 1.2F)), sand));
		}
	}

	/** Gusts, from -1 to 1: the whole storm speeds up and slows down together, and the wind swells with it. */
	private static float gust(ClientLevel level) {
		return Mth.sin(level.getGameTime() * 0.05F);
	}

	public static float windVolume() {
		return WIND_LOOP.shown() + WIND_THUNDER_LOOP.shown() + BLIZZARD_LOOP.shown() + BLIZZARD_THUNDER_LOOP.shown();
	}

	public static float windVolume(Identifier sound) {
		for (AmbientLoop loop : List.of(WIND_LOOP, WIND_THUNDER_LOOP, BLIZZARD_LOOP, BLIZZARD_THUNDER_LOOP)) {
			if (loop.id.equals(sound)) {
				return loop.shown();
			}
		}
		return 0;
	}

	static boolean wind(Identifier sound) {
		return sound.equals(WIND) || sound.equals(WIND_THUNDER) || sound.equals(BLIZZARD) || sound.equals(BLIZZARD_THUNDER);
	}

	public static float density() {
		return density;
	}

	public static float start(float vanilla) {
		return Mth.lerp(density, vanilla, 0);
	}

	public static float end(float vanilla) {
		return MorningMist.eased(density, vanilla, grain == SNOW_GRAIN ? SNOW_END : SAND_END);
	}

	public static float skyEnd(float vanilla) {
		return MorningMist.eased(density, vanilla, SKY_END);
	}

	public static float cloudEnd(float vanilla) {
		return MorningMist.eased(density, vanilla, CLOUD_END);
	}

	public static Vector3fc color(Vector3fc fog) {
		float light = Mth.clamp(Math.max(fog.x(), Math.max(fog.y(), fog.z())) / DAYLIGHT, 0, 1);
		return ARGB.srgbLerp(density * PALE, fog, new Vector3f(color).mul(light));
	}

	/**
	 * How much of the game's snow is gone in a blizzard: its columns scroll straight down and cannot follow the wind, so the
	 * storm's own flakes take over, blowing past with the sand's wind (WeatherEffectRendererMixin). Gone well before the
	 * storm is at its thickest.
	 */
	public static float snowTakenOver() {
		return grain == SNOW_GRAIN ? Math.min(density * 2, 1) : 0;
	}

	/** What is left of what the sky holds (sun, moon, stars), hidden behind the storm. */
	public static float veil() {
		return 1 - density;
	}

	private static final class Grain extends SingleQuadParticle {
		private static final float FADE_TIME = 0.2F;
		private final boolean sand;

		Grain(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, float[] color, Vec3 wind, boolean sand) {
			super(level, x, y, z, sprite);
			this.sand = sand;
			hasPhysics = false;
			gravity = 0;
			friction = 1;
			quadSize = sand ? 0.08F : 0.1F;
			lifetime = Mth.randomBetweenInclusive(random, 25, 40);
			xd = wind.x;
			yd = sand ? random.nextFloat() * 0.06F : -0.05F - random.nextFloat() * 0.05F;
			zd = wind.z;
			setColor(color[0], color[1], color[2]);
			setAlpha(0);
		}

		@Override
		public SingleQuadParticle.Layer getLayer() {
			return SingleQuadParticle.Layer.TRANSLUCENT;
		}

		@Override
		public void tick() {
			super.tick();
			float progress = (float) age / lifetime;
			setAlpha(0.9F * Mth.clamp(Math.min(progress, 1 - progress) / FADE_TIME, 0, 1));
			// Sand hops along the ground and falls back; snow swirls
			yd = sand ? yd - 0.008F : yd + (random.nextFloat() - 0.5F) * 0.04F;
			if (!level.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
				remove();
			}
		}
	}
}
