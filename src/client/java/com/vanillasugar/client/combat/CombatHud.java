package com.vanillasugar.client.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.ClockHud;
import com.vanillasugar.client.map.FramedPanel;
import com.vanillasugar.client.map.SmallText;
import com.vanillasugar.client.quest.QuestButtons;
import com.vanillasugar.combat.HuntersPayload;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.item.VanillaSugarItems;

/**
 * Shows the mobs in a fight to a player wearing a sculk compass: a health bar above the head of those
 * chasing the player or hurt a moment ago, the health just lost draining away behind it, and a "!" that
 * pops over a mob when it spots the player and stays while it chases them. Drawn on the HUD where the
 * mob is on screen, only when the player can see it. An eye in the top left corner, right of the clock's
 * panel, tells mobs are chasing the player, seen or not, with their number when there are several.
 */
public final class CombatHud {
	private static final Identifier BAR_BACKGROUND = VanillaSugar.id("hud/mob_health_background");
	private static final Identifier BAR_PROGRESS = VanillaSugar.id("hud/mob_health_progress");
	private static final Identifier BAR_TRAIL = VanillaSugar.id("hud/mob_health_trail");
	private static final Identifier SPOTTED = VanillaSugar.id("hud/spotted");
	private static final Identifier HUNTED = VanillaSugar.id("hud/hunted");
	/** Keep in sync with tools/combat_textures.py. */
	private static final int BAR_WIDTH = 32;
	private static final int BAR_HEIGHT = 5;
	private static final int SPOTTED_WIDTH = 6;
	private static final int SPOTTED_HEIGHT = 11;
	private static final int EYE_WIDTH = 15;
	private static final int EYE_HEIGHT = 8;
	private static final int COUNT_COLOR = 0xFFE8DABC;
	private static final int MARGIN = 4;
	private static final int GAP = 2;
	private static final double RANGE = 32;
	/** How long the bar stays after the mob was last hurt, without chasing the player. */
	private static final int SHOWN_AFTER_HURT = 100;
	/** The health lost stays a moment before it drains, so the blow can be read. */
	private static final int TRAIL_DELAY = 8;
	private static final float TRAIL_DRAIN = 0.04F;
	/** Ticks the "!" takes to pop in, overshooting before it settles. */
	private static final float POP = 6;

	/** The mobs chasing the player, by entity id, with the tick they spotted them. */
	private static final Map<Integer, Integer> hunters = new HashMap<>();
	/** The mobs around the player, by entity id. */
	private static Map<Integer, Fight> fights = new HashMap<>();
	private static int ticks;

	private static final class Fight {
		float health;
		float trail;
		int lastHurt = -SHOWN_AFTER_HURT;
		boolean visible;
	}

	private CombatHud() {
	}

	public static void receive(HuntersPayload payload) {
		Map<Integer, Integer> spotted = new HashMap<>();
		for (int id : payload.mobs()) {
			spotted.put(id, hunters.getOrDefault(id, ticks));
		}
		hunters.clear();
		hunters.putAll(spotted);
	}

	/** Whether the mob's health bar is drawn. */
	public static boolean isShown(int id) {
		Fight fight = fights.get(id);
		return fight != null && fight.visible;
	}

	/** Whether the mob chases the player, its "!" drawn over its bar. */
	public static boolean hasSpotted(int id) {
		return hunters.containsKey(id);
	}

	public static void reset() {
		hunters.clear();
		fights.clear();
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			reset();
			return;
		}
		ticks++;
		// Without the compass, the server sends no hunters: the last list it sent is stale
		boolean senses = PlayerGear.isWearing(player, VanillaSugarItems.SCULK_COMPASS);
		if (!senses) {
			hunters.clear();
		}
		Map<Integer, Fight> seen = new HashMap<>();
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (!(entity instanceof Mob mob) || mob.distanceToSqr(player) > RANGE * RANGE) {
				continue;
			}
			float health = mob.getHealth();
			Fight fight = fights.get(mob.getId());
			if (fight == null) {
				fight = new Fight();
				fight.health = fight.trail = health;
			}
			if (health < fight.health) {
				fight.lastHurt = ticks;
			}
			fight.health = health;
			if (health >= fight.trail) {
				fight.trail = health;
			} else if (ticks - fight.lastHurt > TRAIL_DELAY) {
				fight.trail = Math.max(health, fight.trail - mob.getMaxHealth() * TRAIL_DRAIN);
			}
			fight.visible = senses && shown(mob, fight) && player.hasLineOfSight(mob);
			seen.put(mob.getId(), fight);
		}
		fights = seen;
	}

	private static boolean shown(Mob mob, Fight fight) {
		return hunters.containsKey(mob.getId()) || ticks - fight.lastHurt < SHOWN_AFTER_HURT;
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || fights.isEmpty()) {
			return;
		}

		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		List<Mob> mobs = new ArrayList<>();
		fights.forEach((id, fight) -> {
			if (fight.visible && minecraft.level.getEntity(id) instanceof Mob mob) {
				mobs.add(mob);
			}
		});
		// The nearest drawn last, in front
		mobs.sort(Comparator.comparingDouble(mob -> -mob.distanceToSqr(player)));

		Camera camera = minecraft.gameRenderer.mainCamera();
		Vec3 eye = camera.position();
		Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());
		for (Mob mob : mobs) {
			// Where a name tag would be, a little higher when the mob has one
			Vec3 target = mob.getPosition(partialTick).add(0, mob.getBbHeight() + (mob.hasCustomName() ? 0.8 : 0.5), 0);
			Vector4f clip = viewProjection.transform(new Vector4f(
					(float) (target.x - eye.x), (float) (target.y - eye.y), (float) (target.z - eye.z), 1));
			if (clip.w <= 0) {
				continue;
			}
			float x = (clip.x / clip.w + 1) / 2 * graphics.guiWidth();
			float y = (1 - clip.y / clip.w) / 2 * graphics.guiHeight();
			if (x < -BAR_WIDTH || x > graphics.guiWidth() + BAR_WIDTH || y < -BAR_WIDTH || y > graphics.guiHeight() + BAR_WIDTH) {
				continue;
			}

			// Smaller far away, never too small to read
			float scale = Mth.clamp(1.25F - (float) Math.sqrt(mob.distanceToSqr(player)) / 32, 0.6F, 1);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			graphics.pose().scale(scale, scale);
			drawBar(graphics, mob, fights.get(mob.getId()));
			Integer spottedAt = hunters.get(mob.getId());
			if (spottedAt != null) {
				drawSpotted(graphics, ticks - spottedAt + partialTick);
			}
			graphics.pose().popMatrix();
		}
	}

	/** Centered on the origin; the outline is drawn by the background, the bars fill its inside. */
	private static void drawBar(GuiGraphicsExtractor graphics, Mob mob, Fight fight) {
		int left = -BAR_WIDTH / 2;
		int top = -BAR_HEIGHT / 2;
		int inside = BAR_WIDTH - 2;
		float max = Math.max(mob.getMaxHealth(), 1);
		int health = Mth.ceil(inside * Mth.clamp(fight.health / max, 0, 1));
		int trail = Mth.ceil(inside * Mth.clamp(fight.trail / max, 0, 1));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND, left, top, BAR_WIDTH, BAR_HEIGHT);
		if (trail > health) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_TRAIL, BAR_WIDTH, BAR_HEIGHT, 0, 0, left, top, 1 + trail, BAR_HEIGHT);
		}
		if (health > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, left, top, 1 + health, BAR_HEIGHT);
		}
	}

	/**
	 * In the top left corner, right of the clock's panel when it is shown: framed like the buttons in the
	 * bottom right corner, the number of mobs in its corner like their key. The eye pops again each time
	 * one more mob spots the player. Drawn after the clock, to know where its panel stops.
	 */
	public static void extractHunted(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isHuntedShown(minecraft)) {
			return;
		}

		int x = ClockHud.right() > 0 ? ClockHud.right() + GAP : MARGIN;
		int y = MARGIN;
		FramedPanel.draw(graphics, x, y, QuestButtons.SIZE, QuestButtons.SIZE);
		int latest = hunters.values().stream().mapToInt(Integer::intValue).max().getAsInt();
		float pop = pop(ticks - latest + deltaTracker.getGameTimeDeltaPartialTick(false));
		graphics.pose().pushMatrix();
		graphics.pose().translate(x + QuestButtons.SIZE / 2, y + QuestButtons.SIZE / 2);
		graphics.pose().scale(pop, pop);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HUNTED, -EYE_WIDTH / 2, -EYE_HEIGHT / 2, EYE_WIDTH, EYE_HEIGHT);
		graphics.pose().popMatrix();
		if (hunters.size() > 1) {
			Font font = minecraft.font;
			Component count = Component.literal(String.valueOf(hunters.size()));
			SmallText.left(graphics, font, count, x + QuestButtons.SIZE - 3 - SmallText.width(font, count),
					y + QuestButtons.SIZE - 3 - SmallText.lineHeight(font), COUNT_COLOR);
		}
	}

	/** Whether the eye shows: mobs are chasing the player, who wears a sculk compass since only then the server sends them. */
	public static boolean isHuntedShown(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		return player != null && !hunters.isEmpty() && !player.isSpectator() && !minecraft.getDebugOverlay().showDebugScreen();
	}

	/** Grows past its size, then settles, over the ticks since it appeared. */
	private static float pop(float age) {
		return age < POP / 2 ? age / (POP / 2) * 1.4F
				: age < POP ? 1.4F - (age - POP / 2) / (POP / 2) * 0.4F
				: 1;
	}

	/** Above the bar, popping up from its foot when the mob has just spotted the player. */
	private static void drawSpotted(GuiGraphicsExtractor graphics, float age) {
		float pop = pop(age);
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, -BAR_HEIGHT / 2F - 2);
		graphics.pose().scale(pop, pop);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPOTTED, -SPOTTED_WIDTH / 2, -SPOTTED_HEIGHT, SPOTTED_WIDTH, SPOTTED_HEIGHT);
		graphics.pose().popMatrix();
	}
}
