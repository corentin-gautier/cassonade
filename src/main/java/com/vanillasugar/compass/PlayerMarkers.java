package com.vanillasugar.compass;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

import com.vanillasugar.VanillaSugar;

/**
 * The markers a player has placed, in every dimension. Stored on the player and synced to its own client.
 * They are placed and removed from the world map; a worn compass also shows them in the world.
 */
public record PlayerMarkers(List<Marker> markers) {
	public static final int MAX = 64;

	// Bright colors first, white last: the player arrow is white
	public static final List<DyeColor> COLORS = List.of(DyeColor.RED, DyeColor.BLUE, DyeColor.LIME, DyeColor.YELLOW, DyeColor.PURPLE,
			DyeColor.ORANGE, DyeColor.CYAN, DyeColor.MAGENTA, DyeColor.GREEN, DyeColor.PINK, DyeColor.LIGHT_BLUE, DyeColor.BROWN,
			DyeColor.BLACK, DyeColor.GRAY, DyeColor.LIGHT_GRAY, DyeColor.WHITE);

	public static final PlayerMarkers EMPTY = new PlayerMarkers(List.of());

	public static final AttachmentType<PlayerMarkers> ATTACHMENT = AttachmentRegistry.create(VanillaSugar.id("markers"), builder -> builder
			.persistent(Marker.CODEC.listOf(0, MAX).xmap(PlayerMarkers::new, PlayerMarkers::markers))
			.copyOnDeath()
			.syncWith(Marker.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)).map(PlayerMarkers::new, PlayerMarkers::markers).cast(),
					AttachmentSyncPredicate.targetOnly()));

	public static void init() {
	}

	public static PlayerMarkers of(Player player) {
		return player.getAttachedOrElse(ATTACHMENT, EMPTY);
	}

	/** Places a marker for the player, telling them in the action bar. */
	public static void place(ServerPlayer player, GlobalPos pos, String name, DyeColor color) {
		PlayerMarkers markers = of(player);
		if (markers.markers().size() >= MAX) {
			player.sendOverlayMessage(Component.translatable("gui.vanillasugar.marker.full", MAX));
		} else {
			player.setAttached(ATTACHMENT, markers.with(pos, name, color));
			player.sendOverlayMessage(name.isEmpty()
					? Component.translatable("gui.vanillasugar.marker.placed", Coordinates.of(pos.pos()))
					: Component.translatable("gui.vanillasugar.marker.placed_named", name, Coordinates.of(pos.pos())));
		}
	}

	public static void remove(ServerPlayer player, Marker marker) {
		player.setAttached(ATTACHMENT, of(player).without(marker));
		player.sendOverlayMessage(Component.translatable("gui.vanillasugar.marker.removed"));
	}

	public PlayerMarkers without(Marker marker) {
		List<Marker> copy = new ArrayList<>(markers);
		copy.remove(marker);
		return new PlayerMarkers(List.copyOf(copy));
	}

	/** The color suggested for a new marker: the next one in turn, so neighbours are told apart. */
	public DyeColor nextColor() {
		return COLORS.get(markers.size() % COLORS.size());
	}

	public PlayerMarkers with(GlobalPos pos, String name, DyeColor color) {
		List<Marker> copy = new ArrayList<>(markers);
		copy.add(new Marker(pos, color, name));
		return new PlayerMarkers(List.copyOf(copy));
	}
}
