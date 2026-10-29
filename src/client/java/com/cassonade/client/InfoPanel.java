package com.cassonade.client;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;

/**
 * The information of a container (a furnace's timers, a trader's departure) as a discreet "!" icon right of its title,
 * styled as the sort and search buttons, whose tooltip lists the rows when hovered.
 */
public final class InfoPanel {
	public static final int LABEL = 0xFFFFD76A;
	public static final int TEXT = 0xFFECD9B2;
	private static final int SEAM = 0xFF1C1109;
	private static final int FILL = 0xFF2E1D1C;
	private static final Identifier ICON = Cassonade.id("container/storage_info");
	private static final Identifier HIGHLIGHTED = Cassonade.id("container/storage_info_highlighted");
	// As the storage buttons (StorageButtons)
	private static final int ICON_SIZE = 11;
	private static final int RIGHT_MARGIN = 7;
	private static final int TOP_MARGIN = 4;

	private InfoPanel() {
	}

	/** A localized subject followed by its value, in the standard text color unless given. */
	public record Entry(Component subject, Component value, int valueColor) {
		public Entry(Component subject, Component value) {
			this(subject, value, TEXT);
		}
	}

	/**
	 * A container's window and its screen, in screen coordinates; originX and originY are where the
	 * graphics' current pose has its origin.
	 */
	public record Window(int left, int top, int width, int height, int screenWidth, int screenHeight, int originX,
			int originY) {
		public Window(int left, int top, int width, int height, int screenWidth, int screenHeight) {
			this(left, top, width, height, screenWidth, screenHeight, 0, 0);
		}
	}

	/** The icon's top-left corner, in screen coordinates: where the storage buttons sit, right of the window's title. */
	public static int[] iconPosition(Window window) {
		return new int[] {window.left() + window.width() - RIGHT_MARGIN - ICON_SIZE, window.top() + TOP_MARGIN};
	}

	/** The tooltip's lines: each row's gold subject followed by its value. */
	public static List<Component> lines(List<Entry> entries) {
		return entries.stream().map(entry -> (Component) Component.empty()
				.append(Component.literal(entry.subject().getString() + ": ").withColor(LABEL))
				.append(entry.value().copy().withColor(entry.valueColor() & 0xFFFFFF))).toList();
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, List<Entry> entries, Window window, int mouseX,
			int mouseY) {
		if (entries.isEmpty()) {
			return;
		}
		int[] position = iconPosition(window);
		boolean hovered = mouseX >= position[0] && mouseX < position[0] + ICON_SIZE && mouseY >= position[1]
				&& mouseY < position[1] + ICON_SIZE;
		int x = position[0] - window.originX();
		int y = position[1] - window.originY();

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? HIGHLIGHTED : ICON, x, y, ICON_SIZE, ICON_SIZE);

		if (hovered) {
			graphics.setComponentTooltipForNextFrame(font, lines(entries), mouseX, mouseY);
		}
	}

	/** A sunk, dark field as the windows' slots, for the text vanilla draws straight on a window. */
	public static void sunk(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
		graphics.fill(x0, y0, x1, y1, SEAM);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, FILL);
	}
}
