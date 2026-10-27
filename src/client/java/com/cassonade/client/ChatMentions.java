package com.cassonade.client;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;

/**
 * A chat message from another player that writes the player's name rings a soft bell, and the name shows
 * in gold in it. The name counts as a word on its own ("@Name" too), whatever its case. Client only: it
 * works on any server.
 */
public final class ChatMentions {
	/** The mod's gold, as the anvil's and the grindstone's figures. */
	private static final int COLOR = 0xFFFFD76A;
	private static final float VOLUME = 0.5F;
	private static final float PITCH = 1.4F;

	/** The text of the mention about to be added to the chat, to recognize it there. */
	private static @Nullable String pending;

	private ChatMentions() {
	}

	public static void init() {
		// Fired just before the message is added to the chat, which ChatComponentMixin hands to highlight()
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, type, timestamp) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || sender != null && sender.id().equals(minecraft.player.getUUID())) {
				return true;
			}
			String body = signed != null ? signed.decoratedContent().getString() : message.getString();
			if (pattern(minecraft).matcher(body).find()) {
				pending = message.getString();
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), PITCH, VOLUME));
			}
			return true;
		});
	}

	/** The message with the player's name in gold, if it is the mention just received; else as it is. */
	public static Component highlight(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (pending == null || minecraft.player == null || !pending.equals(message.getString())) {
			return message;
		}
		pending = null;
		Pattern name = pattern(minecraft);
		MutableComponent highlighted = Component.empty();
		// Rebuilt piece by piece, each keeping its style (links, hover text), the name cut out of them in gold
		message.visit((style, text) -> {
			Matcher matcher = name.matcher(text);
			int from = 0;
			while (matcher.find()) {
				highlighted.append(Component.literal(text.substring(from, matcher.start())).withStyle(style));
				highlighted.append(Component.literal(matcher.group()).withStyle(style.withColor(COLOR)));
				from = matcher.end();
			}
			highlighted.append(Component.literal(text.substring(from)).withStyle(style));
			return Optional.<FormattedText>empty();
		}, Style.EMPTY);
		return highlighted;
	}

	private static Pattern pattern(Minecraft minecraft) {
		String name = minecraft.player.getGameProfile().name();
		return Pattern.compile("(?<![\\w])" + Pattern.quote(name) + "(?![\\w])", Pattern.CASE_INSENSITIVE);
	}
}
