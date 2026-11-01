package com.cassonade.waystone;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/**
 * The waystone: used, it lists the other waystones the player placed or came near and takes the player to the one picked, in any dimension. Its recipe
 * needs a chorus fruit, so fast travel comes once the End is reached, and only ever from one to another.
 */
public final class Waystones {
	private static final int CRYSTAL_LIGHT = 7;
	private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Cassonade.id("waystone"));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Cassonade.id("waystone"));
	// Pistons would carry it away from its entry in the list
	public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY,
			new WaystoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
					.sound(SoundType.STONE).strength(3.5F).requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE).noOcclusion()
					.lightLevel(state -> state.getValue(WaystoneBlock.HALF) == DoubleBlockHalf.UPPER ? CRYSTAL_LIGHT : 0).setId(BLOCK_KEY)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
			new BlockItem(BLOCK, new Item.Properties().useBlockDescriptionPrefix().setId(ITEM_KEY)));
	public static final BlockEntityType<WaystoneCrystalBlockEntity> CRYSTAL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Cassonade.id("waystone_crystal"),
			new BlockEntityType<>(WaystoneCrystalBlockEntity::new, Set.of(BLOCK)));
	private static final double REACH_MARGIN = 2;
	public static final int MAX_NAME_LENGTH = 32;
	/** About as far as the monument can be made out. */
	private static final double DISCOVERY_RADIUS = 32;
	private static final int DISCOVERY_INTERVAL = 20;

	private Waystones() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(output -> output.insertAfter(Items.LODESTONE, ITEM));
		PayloadTypeRegistry.clientboundPlay().register(WaystoneListPayload.TYPE, WaystoneListPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(WaystoneMapPayload.TYPE, WaystoneMapPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(WaystoneMapRequestPayload.TYPE, WaystoneMapRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(WaystoneTravelPayload.TYPE, WaystoneTravelPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(WaystoneRenamePayload.TYPE, WaystoneRenamePayload.STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Waystones::discover);
		ServerPlayNetworking.registerGlobalReceiver(WaystoneMapRequestPayload.TYPE, (payload, context) -> sendToMap(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(WaystoneTravelPayload.TYPE, (payload, context) -> travel(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(WaystoneRenamePayload.TYPE, (payload, context) -> rename(context.player(), payload));
	}

	private static void discover(MinecraftServer server) {
		if (server.getTickCount() % DISCOVERY_INTERVAL != 0) {
			return;
		}
		WaystoneData data = WaystoneData.of(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isSpectator()) {
				for (WaystoneData.Entry entry : List.copyOf(data.entries())) {
					if (entry.pos().dimension() == player.level().dimension() && !entry.discoveredBy().contains(player.getUUID())
							&& entry.pos().pos().closerToCenterThan(player.position(), DISCOVERY_RADIUS)) {
						data.discover(entry, player.getUUID());
					}
				}
			}
		}
	}

	static void openList(ServerPlayer player, BlockPos from) {
		GlobalPos here = GlobalPos.of(player.level().dimension(), from);
		WaystoneData data = WaystoneData.of(player.level().getServer());
		List<WaystoneListPayload.Destination> destinations = data.entries().stream()
				.filter(entry -> !entry.pos().equals(here) && entry.discoveredBy().contains(player.getUUID()))
				.map(entry -> new WaystoneListPayload.Destination(entry.pos(), entry.name())).toList();
		String name = data.at(here).map(WaystoneData.Entry::name).orElse("");
		boolean canRename = data.at(here).filter(entry -> entry.canRename(player.getUUID())).isPresent();
		ServerPlayNetworking.send(player, new WaystoneListPayload(from, name, canRename, destinations));
	}

	/** Only with a map to show them on, the world map or the compass's minimap: without one, the list is for those standing at a waystone. */
	private static void sendToMap(ServerPlayer player) {
		if (PlayerGear.isWearing(player, GearSlot.MAP) || PlayerGear.isWearing(player, GearSlot.COMPASS)) {
			ServerPlayNetworking.send(player, new WaystoneMapPayload(WaystoneData.of(player.level().getServer()).entries().stream()
					.filter(entry -> entry.discoveredBy().contains(player.getUUID()))
					.map(entry -> new WaystoneListPayload.Destination(entry.pos(), entry.name())).toList()));
		}
	}

	/** Blank goes back to the default, the place's dimension and coordinates. */
	private static void rename(ServerPlayer player, WaystoneRenamePayload payload) {
		WaystoneData data = WaystoneData.of(player.level().getServer());
		GlobalPos pos = GlobalPos.of(player.level().dimension(), payload.pos());
		if (player.level().getBlockState(payload.pos()).is(BLOCK) && !player.isSpectator()
				&& player.isWithinBlockInteractionRange(payload.pos(), REACH_MARGIN)
				&& data.at(pos).filter(entry -> entry.canRename(player.getUUID())).isPresent()) {
			String name = StringUtil.filterText(payload.name()).strip();
			data.put(pos, name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH)));
		}
	}

	private static void travel(ServerPlayer player, WaystoneTravelPayload payload) {
		WaystoneData data = WaystoneData.of(player.level().getServer());
		ServerLevel from = player.level();
		ServerLevel level = player.level().getServer().getLevel(payload.to().dimension());
		// Checked again: the list may be old, and only the server can tell the block is still there
		if (level == null || !from.getBlockState(payload.from()).is(BLOCK) || player.isSpectator()
				|| !player.isWithinBlockInteractionRange(payload.from(), REACH_MARGIN)
				|| data.at(payload.to()).filter(entry -> entry.discoveredBy().contains(player.getUUID())).isEmpty()) {
			return;
		}
		BlockPos target = payload.to().pos();
		if (!level.getBlockState(target).is(BLOCK)) {
			data.remove(payload.to());
			player.sendOverlayMessage(Component.translatable("gui.cassonade.waystone.gone"));
		} else {
			Optional<BlockPos> spot = arrival(level, target);
			if (spot.isEmpty()) {
				player.sendOverlayMessage(Component.translatable("gui.cassonade.waystone.blocked"));
				return;
			}
			player.stopRiding();
			Vec3 arrival = Vec3.atBottomCenterOf(spot.get());
			from.playSound(null, player.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1, 1);
			player.teleportTo(level, arrival.x, arrival.y, arrival.z, Set.of(), player.getYRot(), player.getXRot(), true);
			player.resetFallDistance();
			level.playSound(null, spot.get(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1, 1);
		}
	}

	/** At the monument's foot: the first of the eight spots around it with ground and room for a player, on no fluid. */
	private static Optional<BlockPos> arrival(ServerLevel level, BlockPos waystone) {
		return BlockPos.betweenClosedStream(waystone.offset(-1, 0, -1), waystone.offset(1, 0, 1)).map(BlockPos::immutable)
				.filter(pos -> !pos.equals(waystone) && hasRoom(level, pos) && hasRoom(level, pos.above())
						&& level.getFluidState(pos).isEmpty() && !hasRoom(level, pos.below()))
				.findFirst();
	}

	private static boolean hasRoom(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}
}
