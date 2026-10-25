package com.cassonade.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

/**
 * The vanilla map decorations drawn on the minimap, the world map and in the world: the player arrow,
 * marker banners and the treasure map's red X where the player last died (DeathMarker).
 */
public final class MapDecorations {
	private static final Identifier PLAYER = Identifier.withDefaultNamespace("textures/map/decorations/player.png");
	private static final Identifier DEATH = Identifier.withDefaultNamespace("textures/map/decorations/red_x.png");

	private MapDecorations() {
	}

	/** The player's arrow, centered on (x, y) and turned the way it looks. */
	public static void player(GuiGraphicsExtractor graphics, float x, float y, float yRot) {
		player(graphics, x, y, yRot, 1);
	}

	/** The player's arrow, {@code scale} times its 8 pixels. */
	public static void player(GuiGraphicsExtractor graphics, float x, float y, float yRot, float scale) {
		// The arrow texture points north, the player at yaw 0 looks south
		draw(graphics, PLAYER, x, y, yRot + 180, scale);
	}

	/**
	 * A marker's vanilla banner, centered on (x, y), about {@code scale} times its 8 pixels: each of its
	 * pixels a whole number of screen pixels, so it stays sharp at any size. Returns its half height in GUI
	 * pixels.
	 */
	public static float banner(GuiGraphicsExtractor graphics, DyeColor color, float x, float y, float scale) {
		return sharp(graphics, Identifier.withDefaultNamespace("textures/map/decorations/" + color.getSerializedName() + "_banner.png"), x, y, scale);
	}

	/** The red X where the player last died, like a banner. Returns its half height in GUI pixels. */
	public static float death(GuiGraphicsExtractor graphics, float x, float y, float scale) {
		return sharp(graphics, DEATH, x, y, scale);
	}

	/** An 8-pixel decoration centered on (x, y), its scale and place rounded to whole screen pixels. */
	private static float sharp(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float scale) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		float snapped = Math.max(1, Math.round(scale * guiScale)) / (float) guiScale;
		draw(graphics, texture, Math.round(x * guiScale) / (float) guiScale, Math.round(y * guiScale) / (float) guiScale, 0, snapped);
		return 4 * snapped;
	}

	private static void draw(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float rotation) {
		draw(graphics, texture, x, y, rotation, 1);
	}

	private static void draw(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float rotation, float scale) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().rotate(rotation * Mth.DEG_TO_RAD);
		graphics.pose().scale(scale, scale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, -4, -4, 0, 0, 8, 8, 8, 8);
		graphics.pose().popMatrix();
	}
}
