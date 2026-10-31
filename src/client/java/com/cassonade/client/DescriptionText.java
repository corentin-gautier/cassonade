package com.cassonade.client;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The secondary text of every tooltip the mod adds to: smaller than its name (ClientTextTooltipMixin) and in one
 * color, the food and map tooltips' labels included. Lines are recognized by an invisible mark, so that the
 * game's own lines keep their size whatever their color.
 */
public final class DescriptionText {
	/** Dimmer than the enchantments' names, warm like the quest tracker's hint. */
	public static final int COLOR = 0xFF9A8782;
	/** The enchantments' names, the interface's cream, to set them apart from their descriptions. */
	public static final int NAME_COLOR = 0xFFECD9B2;

	/** Marks a line of secondary text. */
	public static final String DESCRIPTION = "cassonade:description";
	/** Marks a description's last line, invisibly, so the tooltip can leave room before what follows. */
	public static final String LAST = "cassonade:description_end";
	/** Marks a line that sets itself apart from what precedes it, as the anvil's penalty does. */
	public static final String SPACED = "cassonade:spaced";
	/** The room left under a description, in pixels at full size. */
	public static final int SPACING = 5;

	private DescriptionText() {
	}

	/** A tooltip's line of secondary text. */
	public static Component of(Component line) {
		return line.copy().withStyle(style -> style.withColor(COLOR).withInsertion(DESCRIPTION));
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

	/** Whether the line carries one of the marks above. */
	public static boolean is(FormattedCharSequence line) {
		boolean[] result = {false};
		line.accept((index, style, codePoint) -> {
			String insertion = style.getInsertion();
			result[0] = DESCRIPTION.equals(insertion) || LAST.equals(insertion) || SPACED.equals(insertion);
			return false;
		});
		return result[0];
	}
}
