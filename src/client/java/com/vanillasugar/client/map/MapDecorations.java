package com.vanillasugar.client.map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

/** The vanilla map decorations drawn on the minimap and the world map: the player arrow and marker banners. */
public final class MapDecorations {
	private static final Identifier PLAYER = Identifier.withDefaultNamespace("textures/map/decorations/player.png");

	private MapDecorations() {
	}

	/** The player's arrow, centered on (x, y) and turned the way it looks. */
	public static void player(GuiGraphicsExtractor graphics, float x, float y, float yRot) {
		// The arrow texture points north, the player at yaw 0 looks south
		draw(graphics, PLAYER, x, y, yRot + 180);
	}

	public static void banner(GuiGraphicsExtractor graphics, DyeColor color, float x, float y) {
		banner(graphics, color, x, y, 1);
	}

	/** A marker's banner, centered on (x, y), {@code scale} times its 8 pixels. */
	public static void banner(GuiGraphicsExtractor graphics, DyeColor color, float x, float y, float scale) {
		draw(graphics, Identifier.withDefaultNamespace("textures/map/decorations/" + color.getSerializedName() + "_banner.png"), x, y, 0, scale);
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
