package com.cassonade.client.map;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;
import com.cassonade.ServerSupport;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/**
 * Coordinates written in the chat ("120 64 -300", "x: 120, y: 64, z: -300") become a link while the world map
 * is worn: a click opens the map there, with the box to name a marker on that spot. The click is a custom
 * click event the client keeps for itself (ScreenMixin), never sent to the server. Markers need the mod
 * on the server, so does the link.
 */
public final class ChatCoordinates {
	public static final Identifier CLICK = Cassonade.id("place_marker");
	/** The mod's gold, as the mentions of the player's name (ChatMentions). */
	private static final int COLOR = 0xFFFFD76A;
	/** Three whole numbers, each perhaps labelled "x", "y", "z", apart by spaces or commas; a height of at most four digits. */
	private static final Pattern COORDINATES = Pattern.compile(
			"(?<![\\w.-])(?:x\\s*[:=]?\\s*)?(-?\\d{1,8})[\\s,;]+(?:y\\s*[:=]?\\s*)?(-?\\d{1,4})[\\s,;]+(?:z\\s*[:=]?\\s*)?(-?\\d{1,8})(?![\\w.])",
			Pattern.CASE_INSENSITIVE);

	private ChatCoordinates() {
	}

	/** The message with its coordinates made links, when the player can place a marker; else as it is. */
	public static Component link(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !PlayerGear.isWearing(minecraft.player, GearSlot.MAP) || !ServerSupport.onClient()
				|| !COORDINATES.matcher(message.getString()).find()) {
			return message;
		}
		MutableComponent linked = Component.empty();
		// Rebuilt piece by piece, each keeping its style; coordinates split across two styles stay plain text
		message.visit((style, text) -> {
			Matcher matcher = COORDINATES.matcher(text);
			int from = 0;
			while (matcher.find()) {
				linked.append(Component.literal(text.substring(from, matcher.start())).withStyle(style));
				linked.append(Component.literal(matcher.group()).withStyle(linkStyle(style,
						Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(3)))));
				from = matcher.end();
			}
			linked.append(Component.literal(text.substring(from)).withStyle(style));
			return Optional.<FormattedText>empty();
		}, Style.EMPTY);
		return linked;
	}

	private static Style linkStyle(Style style, int x, int z) {
		CompoundTag at = new CompoundTag();
		at.putInt("x", x);
		at.putInt("z", z);
		return style.withColor(COLOR).withUnderlined(true)
				.withClickEvent(new ClickEvent.Custom(CLICK, Optional.of(at)))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable("gui.cassonade.marker.from_chat")));
	}

	/** A click on a link: the world map, ready to name a marker there. */
	public static void clicked(Optional<Tag> payload) {
		if (payload.orElse(null) instanceof CompoundTag at) {
			Minecraft.getInstance().gui.setScreen(WorldMapScreen.placingAt(at.getIntOr("x", 0), at.getIntOr("z", 0)));
		}
	}
}
