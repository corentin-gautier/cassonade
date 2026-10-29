package com.cassonade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;

import com.cassonade.client.map.SmallText;

/** Recognizes the enchantment descriptions in tooltips, which are drawn smaller (ClientTextTooltipMixin). */
public final class DescriptionText {
	/** Dimmer than the enchantments' gray names, warm like the quest tracker's hint. */
	public static final int COLOR = 0xFF9A8782;

	/** Marks a description's last line, invisibly, so the tooltip can leave room before what follows. */
	public static final String LAST = "cassonade:description_end";
	/** Marks a line that sets itself apart from what precedes it, as the anvil's penalty does. */
	public static final String SPACED = "cassonade:spaced";
	/** The room left under a description, in pixels at full size. */
	public static final int SPACING = 5;

	private DescriptionText() {
	}

	/** Two GUI scale steps smaller than usual, one when the GUI scale is too low for that to stay readable. */
	public static float scale() {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		return guiScale <= 2 ? SmallText.scale() : (guiScale - 2f) / guiScale;
	}

	/** Whether the line asks for room above it. */
	public static boolean isSpaced(FormattedCharSequence line) {
		boolean[] result = {false};
		line.accept((index, style, codePoint) -> {
			result[0] = SPACED.equals(style.getInsertion());
			return false;
		});
		return result[0];
	}

	/** Whether the line is the last of a description. */
	public static boolean isLast(FormattedCharSequence line) {
		boolean[] result = {false};
		line.accept((index, style, codePoint) -> {
			result[0] = LAST.equals(style.getInsertion());
			return false;
		});
		return result[0];
	}

	/** Whether the line starts with the description's color: only descriptions have it. */
	public static boolean is(FormattedCharSequence line) {
		boolean[] result = {false};
		line.accept((index, style, codePoint) -> {
			result[0] = style.getColor() != null && (style.getColor().getValue() | 0xFF000000) == COLOR;
			return false;
		});
		return result[0];
	}
}
