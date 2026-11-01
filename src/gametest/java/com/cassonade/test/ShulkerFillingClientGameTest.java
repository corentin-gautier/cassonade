package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import com.cassonade.client.storage.ShulkerMouseActions;
import com.cassonade.storage.ShulkerFilling;

/** A Shulker box clicks, drags and scrolls like a bundle. */
public class ShulkerFillingClientGameTest implements FabricClientGameTest {
	// Menu slots of the inventory's top row, then of the row under it
	private static final int BOX = 9;
	private static final int STONE = 10;
	private static final int DIRT = 11;
	private static final int EMPTY = 12;
	private static final int OTHER_BOX = 18;
	private static final int SWEEP_A = 19;
	private static final int SWEEP_B = 20;
	private static final Item RED_BOX = Items.DYED_SHULKER_BOX.red();

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.getInventory().clearContent();
				player.getInventory().setItem(BOX, new ItemStack(Items.SHULKER_BOX));
				player.getInventory().setItem(STONE, new ItemStack(Items.STONE, 40));
				player.getInventory().setItem(DIRT, new ItemStack(Items.DIRT, 10));
				player.getInventory().setItem(OTHER_BOX, new ItemStack(RED_BOX));
				player.getInventory().setItem(SWEEP_A, new ItemStack(Items.SAND, 3));
				player.getInventory().setItem(SWEEP_B, new ItemStack(Items.GRAVEL, 3));
			});
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
			context.waitForScreen(InventoryScreen.class);

			// The box on the cursor: a left or a right click on a stack puts it in
			click(context, BOX, 0);
			click(context, STONE, 0);
			checkCarried(world, 40, "a left click on a stack");
			check(context.computeOnClient(client -> count(client.player.containerMenu.getCarried())) == 40, "the client agrees");
			click(context, DIRT, 1);
			checkCarried(world, 50, "a right click on a stack");
			checkSlot(world, DIRT, Items.AIR, "the dirt gone");

			// A box doesn't go in a box, and no click swaps them
			click(context, OTHER_BOX, 1);
			click(context, OTHER_BOX, 0);
			checkSlot(world, OTHER_BOX, RED_BOX, "the other box where it was");
			checkCarried(world, 50, "the box still on the cursor");

			// A right click on an empty slot takes out the last stack in
			click(context, EMPTY, 1);
			checkSlot(world, EMPTY, Items.DIRT, "the dirt out in the slot");
			checkCarried(world, 40, "the dirt out of the box");

			// The box in its slot: a stack from the cursor goes in, a right click with an empty cursor takes one out
			click(context, BOX, 0);
			click(context, EMPTY, 0);
			click(context, BOX, 0);
			checkInSlot(world, BOX, 50, "the stack on the cursor in the box");
			Screenshots.take(context, "cassonade-shulker-bar");
			check(context.computeOnClient(client -> client.player.getInventory().getItem(BOX).isBarVisible()), "the fill bar on the box");
			click(context, BOX, 1);
			checkInSlot(world, BOX, 40, "the stack out on the cursor");
			world.getServer().runOnServer(server -> check(player(server).containerMenu.getCarried().is(Items.DIRT), "the dirt on the cursor"));
			click(context, BOX, 0);

			// The wheel picks what comes out: the first stack, the stone, rather than the last
			context.runOnClient(client -> {
				ItemStack box = client.player.getInventory().getItem(BOX);
				ShulkerMouseActions wheel = new ShulkerMouseActions(client);
				wheel.onMouseScrolled(0, 1, BOX, box);
				if (ShulkerFilling.selected(client.player) != 0) {
					wheel.onMouseScrolled(0, 1, BOX, box);
				}
				check(ShulkerFilling.selected(client.player) == 0, "the first stack picked");
			});
			context.waitTicks(5);
			click(context, BOX, 1);
			world.getServer().runOnServer(server -> check(player(server).containerMenu.getCarried().is(Items.STONE), "the stack picked comes out"));
			check(context.computeOnClient(client -> ShulkerFilling.selected(client.player)) == -1, "the pick forgotten after");
			click(context, STONE, 0);

			// Dragging with the box: over stacks it fills
			click(context, BOX, 0);
			context.runOnClient(client -> {
				client.gui.screen().mouseClicked(ScreenInput.slotEvent(client, SWEEP_A, 0), false);
				client.gui.screen().mouseDragged(ScreenInput.slotEvent(client, SWEEP_B, 0), 1, 1);
				client.gui.screen().mouseReleased(ScreenInput.slotEvent(client, SWEEP_B, 0));
			});
			context.waitTicks(5);
			checkSlot(world, SWEEP_A, Items.AIR, "the first stack dragged over in the box");
			checkSlot(world, SWEEP_B, Items.AIR, "the second too");
			checkCarried(world, 6 + 10, "the box holds what it dragged over");
			click(context, BOX, 0);
			context.setScreen(() -> null);
		}
	}

	private static void checkCarried(TestSingleplayerContext world, int items, String what) {
		world.getServer().runOnServer(server -> check(count(player(server).containerMenu.getCarried()) == items,
				what + ": " + count(player(server).containerMenu.getCarried())));
	}

	private static void checkInSlot(TestSingleplayerContext world, int slot, int items, String what) {
		world.getServer().runOnServer(server -> check(count(player(server).getInventory().getItem(slot)) == items, what));
	}

	private static void checkSlot(TestSingleplayerContext world, int slot, Item item, String what) {
		world.getServer().runOnServer(server -> check(player(server).getInventory().getItem(slot).is(item), what));
	}

	private static int count(ItemStack box) {
		return box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().mapToInt(ItemStack::getCount).sum();
	}

	private static void click(ClientGameTestContext context, int slot, int button) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot, button, ContainerInput.PICKUP, client.player));
		context.waitTicks(5);
	}
}
