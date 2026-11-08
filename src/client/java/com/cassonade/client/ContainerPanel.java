package com.cassonade.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * A wooden window of any size, as the game's containers are drawn (tools/gui_style.py): cut out of the horse's window,
 * its rounded corners kept, its edges and its grain repeated, so a screen of the mod that is used like a container looks
 * like one, and follows a resource pack that redraws them.
 */
public final class ContainerPanel {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/horse.png");
	private static final int TEXTURE_SIZE = 256;
	/** The window in its texture. */
	private static final int WIDTH = 176;
	private static final int HEIGHT = 166;
	/** The rounded corners, with the dark outline and the light edge inside it. */
	private static final int CORNER = 4;
	/** A patch of plain wood, between the corner and the mount's box, repeated over the middle. */
	private static final int GRAIN_U = 4;
	private static final int GRAIN_V = 4;
	private static final int GRAIN_WIDTH = 20;
	private static final int GRAIN_HEIGHT = 12;
	/** The sunk boxes, as the slots and the mount's box: outline, then inside. */
	public static final int SLOT_OUTLINE = 0xFF1E0D0E;
	public static final int SLOT_FILL = 0xFF2E1D1C;
	public static final int BOX_FILL = 0xFF160608;
	/** The titles, cream on the wood (ContainerLabelsMixin). */
	public static final int LABEL = 0xFFECD9B2;

	private ContainerPanel() {
	}

	public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		int right = x + width - CORNER;
		int bottom = y + height - CORNER;
		// The middle, then the edges and the corners over it
		tile(graphics, x + CORNER, y + CORNER, width - 2 * CORNER, height - 2 * CORNER, GRAIN_U, GRAIN_V, GRAIN_WIDTH, GRAIN_HEIGHT);
		int edgeWidth = WIDTH - 2 * CORNER;
		int edgeHeight = HEIGHT - 2 * CORNER;
		tile(graphics, x + CORNER, y, width - 2 * CORNER, CORNER, CORNER, 0, edgeWidth, CORNER);
		tile(graphics, x + CORNER, bottom, width - 2 * CORNER, CORNER, CORNER, HEIGHT - CORNER, edgeWidth, CORNER);
		tile(graphics, x, y + CORNER, CORNER, height - 2 * CORNER, 0, CORNER, CORNER, edgeHeight);
		tile(graphics, right, y + CORNER, CORNER, height - 2 * CORNER, WIDTH - CORNER, CORNER, CORNER, edgeHeight);
		blit(graphics, x, y, 0, 0, CORNER, CORNER);
		blit(graphics, right, y, WIDTH - CORNER, 0, CORNER, CORNER);
		blit(graphics, x, bottom, 0, HEIGHT - CORNER, CORNER, CORNER);
		blit(graphics, right, bottom, WIDTH - CORNER, HEIGHT - CORNER, CORNER, CORNER);
	}

	/** A sunk box, as an inventory slot, or darker as the mount's box. */
	public static void sunk(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill) {
		graphics.fill(x, y, x + width, y + height, SLOT_OUTLINE);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
	}

	/** Covers the area with the patch repeated, the last ones cut short. */
	private static void tile(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int u, int v, int patchWidth, int patchHeight) {
		for (int dy = 0; dy < height; dy += patchHeight) {
			for (int dx = 0; dx < width; dx += patchWidth) {
				blit(graphics, x + dx, y + dy, u, v, Math.min(patchWidth, width - dx), Math.min(patchHeight, height - dy));
			}
		}
	}

	private static void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
	}
}
