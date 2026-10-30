package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import com.cassonade.client.BackpackPanel;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.mixin.AbstractRecipeBookScreenAccessor;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.gear.ElytraGear;
import com.cassonade.gear.GearLayout;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * Equipment in its gear slots: shift clicks equip the backpack, compass and world map, the backpack takes stacks
 * when open, its key, tab and strap open and close it; the recipe book's tab and ribbon; an elytra worn with
 * a chestplate, mended and gliding. All of it kept through a reload, and dropped on death.
 */
public class GearClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		// A common player's window, 1080p with the automatic GUI scale, rather than the small default
		context.getInput().resizeWindow(1920, 1080);
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// Declines the quest journal offered in each new world, which would cover the screens below
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("give @a cassonade:backpack");
			world.getServer().runCommand("give @a compass");
			world.getServer().runCommand("give @a cassonade:world_map");
			world.getServer().runCommand("give @a diamond 64");
			world.getServer().runCommand("give @a oak_planks 64");
			world.getServer().runCommand("give @a diamond_chestplate");
			context.waitTicks(5);

			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			Screenshots.take(context, "cassonade-1-inventory");

			// Hotbar 0..2: backpack, compass, map -> shift-click equips them
			for (int hotbar = 0; hotbar < 3; hotbar++) {
				ScreenInput.quickMove(context, InventoryMenu.USE_ROW_SLOT_START + hotbar);
			}
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-2-gear-equipped");

			// Backpack closed: diamonds move from the hotbar to the main inventory, like vanilla
			ScreenInput.quickMove(context, InventoryMenu.USE_ROW_SLOT_START + 3);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(PlayerGear.of(player).backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream()
						.findAny().isEmpty(), "closed backpack left empty");
				check(player.getInventory().getItem(InventoryMenu.INV_SLOT_START).is(Items.DIAMOND), "diamonds moved to the main inventory");
			});

			// Backpack open: diamonds, planks go into it, the chestplate still goes to the armor slot
			context.runOnClient(client -> BackpackPanel.setOpen(true));
			context.waitTicks(2);
			ScreenInput.quickMove(context, InventoryMenu.INV_SLOT_START);
			for (int hotbar = 4; hotbar < 6; hotbar++) {
				ScreenInput.quickMove(context, InventoryMenu.USE_ROW_SLOT_START + hotbar);
			}
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.runOnClient(client -> client.gui.toastManager().clear());
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-3-backpack-filled");

			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				PlayerGear gear = PlayerGear.of(player);
				check(gear.compass().is(Items.COMPASS), "compass equipped");
				check(gear.map().is(CassonadeItems.WORLD_MAP), "map equipped");
				check(!GearSlot.MAP.accepts(new ItemStack(Items.MAP)) && !GearSlot.MAP.accepts(new ItemStack(Items.FILLED_MAP)),
						"only a world map fits the map slot");
				ItemContainerContents contents = gear.backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
				check(contents.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 64), "diamonds in backpack");
				check(contents.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.OAK_PLANKS)), "planks in backpack");
				check(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "chestplate worn");
			});
			// With the backpack open, a shift click dragged over the inventory's stacks moves them all into it, and back
			world.getServer().runCommand("item replace entity @a inventory.20 with brick 5");
			world.getServer().runCommand("item replace entity @a inventory.21 with clay_ball 5");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			ScreenInput.shiftSweep(context, InventoryMenu.INV_SLOT_START + 20, InventoryMenu.INV_SLOT_START + 21, true);
			world.getConnection().waitForServerboundPackets();
			check(context.computeOnClient(client -> client.player.getInventory().countItem(Items.BRICK) == 0
					&& client.player.getInventory().countItem(Items.CLAY_BALL) == 0), "a shift click dragged fills the backpack");
			int[] packed = context.computeOnClient(client -> {
				List<Slot> slots = client.player.inventoryMenu.slots;
				int brick = -1;
				int clay = -1;
				for (int i = GearLayout.BACKPACK_START; i < GearLayout.BACKPACK_END; i++) {
					brick = slots.get(i).getItem().is(Items.BRICK) ? i : brick;
					clay = slots.get(i).getItem().is(Items.CLAY_BALL) ? i : clay;
				}
				return new int[] {brick, clay};
			});
			ScreenInput.shiftSweep(context, packed[0], packed[1], true);
			world.getConnection().waitForServerboundPackets();
			check(context.computeOnClient(client -> client.player.getInventory().countItem(Items.BRICK) == 5
					&& client.player.getInventory().countItem(Items.CLAY_BALL) == 5), "a shift click dragged empties the backpack");

			// The backpack key opens the inventory on the backpack, with the map tab and the marker display button
			context.runOnClient(client -> BackpackPanel.setOpen(false));
			context.getInput().pressKey(CassonadeClient.OPEN_BACKPACK);
			context.waitForScreen(InventoryScreen.class);
			context.waitTicks(2);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "backpack key opens the backpack");
			Screenshots.take(context, "cassonade-10-backpack-key");
			// Its tab, the second along the top after the recipe book's, closes it and the inventory stays
			// open; the key opens it again
			ScreenInput.click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + GearLayout.PANEL_SHIFT + GearLayout.tabX(1) + GearLayout.TAB_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.TAB_Y + 8);
			check(!context.computeOnClient(client -> BackpackPanel.isOpen()), "the backpack tab closes the backpack");
			check(context.computeOnClient(client -> client.gui.screen() instanceof InventoryScreen), "the backpack tab leaves the inventory open");
			context.getInput().pressKey(CassonadeClient.OPEN_BACKPACK);
			context.waitTicks(2);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "backpack key opens the backpack in the inventory");
			// The strap on top of the panel closes it too
			ScreenInput.click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + GearLayout.PANEL_SHIFT + GearLayout.STRAP_X + GearLayout.STRAP_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.STRAP_Y + 2);
			check(!context.computeOnClient(client -> BackpackPanel.isOpen()), "the strap closes the backpack");
			context.getInput().pressKey(CassonadeClient.OPEN_BACKPACK);
			context.waitTicks(2);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "backpack key opens the backpack again");
			context.getInput().pressKey(CassonadeClient.OPEN_BACKPACK);
			context.waitTicks(2);
			check(context.computeOnClient(client -> client.gui.screen() == null), "backpack key closes the inventory");
			check(!context.computeOnClient(client -> BackpackPanel.isOpen()), "closing the inventory closes the backpack");

			// The recipe book tab, first along the top of the inventory, opens the book, which moves the
			// inventory to the right: the tabs must follow it; the book's ribbon closes it
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			ScreenInput.click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + GearLayout.tabX(0) + GearLayout.TAB_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.TAB_Y + 8);
			check(context.computeOnClient(GearClientGameTest::recipeBookOpen), "the recipe book tab opens the book");
			Screenshots.take(context, "cassonade-11-recipe-book-tabs");
			// Every recipe known: pages enough for the arrows below them
			world.getServer().runCommand("recipe give @a *");
			context.waitTicks(10);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-11-recipe-book-pages");
			ScreenInput.click(context, client -> (client.getWindow().getGuiScaledWidth() - 147) / 2 - 86 + GearLayout.RIBBON_X + GearLayout.RIBBON_WIDTH / 2,
					client -> (client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.RIBBON_Y + 3);
			check(!context.computeOnClient(GearClientGameTest::recipeBookOpen), "the ribbon closes the recipe book");

			// An elytra with Mending goes to its gear slot, the chestplate still worn; experience repairs it
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("item replace entity @a hotbar.6 with elytra[enchantments={\"minecraft:mending\":1},damage=200]");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			ScreenInput.quickMove(context, InventoryMenu.USE_ROW_SLOT_START + 6);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-16-elytra-slot");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(ElytraGear.worn(player).is(Items.ELYTRA), "elytra equipped in its gear slot");
				check(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "chestplate worn with the elytra");
			});
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run summon experience_orb ~ ~ ~ {Value:20}");
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(ElytraGear.worn(server.getPlayerList().getPlayers().getFirst()).getDamageValue() < 200,
					"Mending repairs the elytra of the gear slot"));

			// High up, it glides and wears down; other players see its wings
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~60 ~ 0 0");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> check(server.getPlayerList().getPlayers().getFirst().tryToStartFallFlying(),
					"glides with the elytra of the gear slot"));
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(25);
			Screenshots.take(context, "cassonade-16-elytra-gliding");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.isFallFlying(), "still gliding");
				check(ElytraGear.worn(player).getDamageValue() > 0, "gliding wears the elytra down");
			});
			check(context.computeOnClient(client -> ElytraGear.shown(client.player).is(Items.ELYTRA)), "the worn elytra synced for the wings");
			context.setScreen(() -> null);
			save = world.getWorldSave();
		}

		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runOnServer(server -> {
				PlayerGear gear = PlayerGear.of(server.getPlayerList().getPlayers().getFirst());
				check(gear.compass().is(Items.COMPASS) && gear.map().is(CassonadeItems.WORLD_MAP) && gear.elytra().is(Items.ELYTRA), "gear kept after reloading the world");
				check(gear.backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream()
						.anyMatch(stack -> stack.is(Items.DIAMOND)), "backpack contents kept after reloading the world");
			});
			check(context.computeOnClient(client -> PlayerGear.isWearing(client.player, GearSlot.COMPASS)), "gear synced to the client");

			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("kill @a");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				for (GearSlot slot : GearSlot.values()) {
					check(!PlayerGear.isWearing(player, slot), "gear removed on death");
				}
				check(!player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64),
						item -> item.getItem().is(CassonadeItems.BACKPACK)).isEmpty(), "backpack dropped on death");
			});
		}
	}

	private static boolean recipeBookOpen(Minecraft client) {
		return client.gui.screen() instanceof InventoryScreen screen
				&& ((AbstractRecipeBookScreenAccessor) screen).cassonade$getRecipeBookComponent().isVisible();
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
