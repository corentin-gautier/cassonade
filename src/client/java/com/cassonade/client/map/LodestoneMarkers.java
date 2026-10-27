package com.cassonade.client.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;

import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/**
 * The lodestones the player's compasses point to, shown on the minimap and the world map like markers of
 * their own: every lodestone compass carried, in the inventory or worn in the compass's slot, adds its
 * lodestone in this dimension. A compass renamed at the anvil names it. The compass knows its lodestone
 * on its own, so it works on any server.
 */
public final class LodestoneMarkers {
	private LodestoneMarkers() {
	}

	public record Lodestone(BlockPos pos, Component name) {
	}

	public static List<Lodestone> here(Player player) {
		List<ItemStack> carried = new ArrayList<>(player.getInventory().getNonEquipmentItems());
		carried.add(player.getOffhandItem());
		carried.add(PlayerGear.of(player).get(GearSlot.COMPASS));
		// Two compasses on the same lodestone make one marker, named after the first
		Map<BlockPos, Lodestone> found = new LinkedHashMap<>();
		for (ItemStack stack : carried) {
			LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
			if (tracker == null || tracker.target().isEmpty()) {
				continue;
			}
			GlobalPos target = tracker.target().get();
			if (target.dimension() == player.level().dimension()) {
				Component name = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName() : Component.translatable("block.minecraft.lodestone");
				found.putIfAbsent(target.pos(), new Lodestone(target.pos(), name));
			}
		}
		return List.copyOf(found.values());
	}
}
