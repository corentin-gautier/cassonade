package com.cassonade.test;

import java.util.List;
import java.util.function.ToIntFunction;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.NewItems;
import com.cassonade.client.PickedUpItems;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.gear.GearLayout;
import com.cassonade.storage.LockedSlots;

/**
 * The inventory's own handling, one case at a time: the food in hand blinking on the hunger bar, a shift click
 * dragged right after clicking a tab, the wheel moving one item, the list of items picked up and their dot,
 * the drop key sparing a locked slot, the rows rotated into the hotbar.
 */
public class InventoryClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");

			// Bread in hand on a half-empty hunger bar: the drumsticks it would restore blink on the bar
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(9));
			world.getServer().runCommand("item replace entity @a weapon.mainhand with bread");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-food-preview");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");

			world.getServer().runCommand("item replace entity @a inventory.0 with brick 5");
			world.getServer().runCommand("item replace entity @a inventory.1 with clay_ball 5");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);

			// The recipe book's tab, then at once a shift click dragged over two stacks: both go to the hotbar
			click(context, client -> leftPos(client) + GearLayout.tabX(0) + GearLayout.TAB_WIDTH / 2,
					client -> topPos(client) + GearLayout.TAB_Y + 8);
			shiftSweep(context, InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_START + 1);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			String moved = context.computeOnClient(client -> client.player.inventoryMenu.getSlot(InventoryMenu.INV_SLOT_START).getItem() + " / "
					+ client.player.inventoryMenu.getSlot(InventoryMenu.INV_SLOT_START + 1).getItem());
			check(moved.equals("0 minecraft:air / 0 minecraft:air"), "the first shift click dragged after the recipe tab moves both stacks (" + moved + ")");

			// Scrolling down over a stack moves one item of it, to the hotbar here; scrolling up does nothing
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a inventory.0 with brick 5");
			context.waitTicks(2);
			context.runOnClient(client -> {
				MouseButtonEvent over = slotEvent(client, InventoryMenu.INV_SLOT_START);
				client.gui.screen().mouseScrolled(over.x(), over.y(), 0, -1);
				client.gui.screen().mouseScrolled(over.x(), over.y(), 0, -1);
				client.gui.screen().mouseScrolled(over.x(), over.y(), 0, 1);
			});
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			String scrolled = context.computeOnClient(client -> client.player.inventoryMenu.getSlot(InventoryMenu.INV_SLOT_START).getItem().getCount()
					+ " / " + client.player.getInventory().countItem(Items.BRICK) + " / " + client.player.inventoryMenu.getCarried().isEmpty());
			check(scrolled.equals("3 / 5 / true"), "two notches down move two bricks out of their stack, none lost (" + scrolled + ")");

			// What the player picks up, listed on the right, the same item's pickups added up, gone after a moment
			context.setScreen(() -> null);
			// A book already there, then pickups into the empty hotbar
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a inventory.0 with book");
			world.getServer().runCommand("execute at @a run summon item ~ ~ ~ {Item:{id:\"minecraft:cobblestone\",count:4}}");
			world.getServer().runCommand("execute at @a run summon item ~ ~ ~ {Item:{id:\"minecraft:dirt\",count:1}}");
			context.waitTicks(10);
			world.getServer().runCommand("execute at @a run summon item ~ ~ ~ {Item:{id:\"minecraft:cobblestone\",count:6}}");
			context.waitTicks(10);
			List<String> picked = context.computeOnClient(client -> PickedUpItems.shown());
			check(picked.equals(List.of("+1 Dirt", "+10 Cobblestone")), "the pickups listed, cobblestone added up and last (" + picked + ")");
			Screenshots.take(context, "cassonade-picked-up");

			// What was picked up carries a dot in the inventory, until hovered
			check(context.computeOnClient(client -> NewItems.isMarked(0) && NewItems.isMarked(1)), "the cobblestone and the dirt picked up are marked");
			check(!context.computeOnClient(client -> NewItems.isMarked(9)), "the book already there is not marked");
			// Without the banner of the backpack's advancement over the slots
			context.runOnClient(client -> {
				AchievementBanner.clear();
				client.gui.toastManager().clear();
			});
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			Screenshots.take(context, "cassonade-new-items");
			hover(context, InventoryMenu.USE_ROW_SLOT_START);
			context.waitTicks(2);
			check(context.computeOnClient(client -> !NewItems.isMarked(0) && NewItems.isMarked(1)), "the stack hovered no longer marked, the other still");
			context.setScreen(() -> null);
			// Its 3 seconds, and a little more: the wait ends as soon as it is gone
			context.waitFor(client -> PickedUpItems.shown().isEmpty(), 100);

			world.getServer().runCommand("item replace entity @a inventory.9 with book");
			world.getConnection().waitForClientboundPackets();
			context.runOnClient(client -> {
				NewItems.reset();
				NewItems.pickedUp(new ItemStack(Items.BOOK));
				NewItems.tick(client);
			});
			check(!context.computeOnClient(client -> NewItems.isMarked(9)), "the initial inventory snapshot does not mark a pre-existing stack");

			// The drop key leaves a locked stack alone, in hand or in a window, and throws it once unlocked
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a hotbar.0 with stone 4");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().setSelectedSlot(0);
				player.setAttached(LockedSlots.ATTACHMENT, LockedSlots.NONE.toggled(0));
			});
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyDrop);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> client.player.getMainHandItem().getCount()) == 4, "a locked stack kept in hand by the drop key");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			hover(context, InventoryMenu.USE_ROW_SLOT_START);
			context.getInput().pressKey(options -> options.keyDrop);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> client.player.getMainHandItem().getCount()) == 4, "a locked stack kept in the window by the drop key");
			context.setScreen(() -> null);
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setAttached(LockedSlots.ATTACHMENT, LockedSlots.NONE));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyDrop);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> client.player.getMainHandItem().getCount()) == 3, "an unlocked stack thrown one by one");

			// The rotation key brings the row above the hotbar down into it, and sends the hotbar to the top row
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a hotbar.0 with stone 4");
			world.getServer().runCommand("item replace entity @a inventory.18 with dirt 2");
			world.getServer().runCommand("item replace entity @a hotbar.1 with torch 3");
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
					.setAttached(LockedSlots.ATTACHMENT, LockedSlots.NONE.toggled(1)));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.getInput().pressKey(CassonadeClient.ROTATE_HOTBAR);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			String rotated = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return player.getInventory().getItem(0) + " / " + player.getInventory().getItem(9) + " / " + player.getInventory().getItem(1);
			});
			check(rotated.equals("2 minecraft:dirt / 4 minecraft:stone / 3 minecraft:torch"),
					"the rows rotated into the hotbar, the locked column left alone (" + rotated + ")");
			for (int i = 0; i < 3; i++) {
				context.getInput().pressKey(CassonadeClient.ROTATE_HOTBAR);
				world.getConnection().waitForServerboundPackets();
				context.waitTicks(2);
			}
			String back = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().getInventory().getItem(0).toString());
			check(back.equals("4 minecraft:stone"), "four rotations bring the hotbar back (" + back + ")");
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setAttached(LockedSlots.ATTACHMENT, LockedSlots.NONE));
		}
	}

	private static int leftPos(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).cassonade$getLeftPos();
	}

	private static int topPos(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).cassonade$getTopPos();
	}

	private static void click(ClientGameTestContext context, ToIntFunction<Minecraft> x, ToIntFunction<Minecraft> y) {
		context.runOnClient(client -> {
			MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
			client.gui.screen().mouseClicked(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left), false);
			client.gui.screen().mouseReleased(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left));
		});
		context.waitTicks(2);
	}

	/** The cursor over a slot of the open window, for its tooltip. */
	private static void hover(ClientGameTestContext context, int slotIndex) {
		double[] center = context.computeOnClient(client -> {
			Slot slot = ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().getSlot(slotIndex);
			double scale = client.getWindow().getGuiScale();
			return new double[] {(leftPos(client) + slot.x + 8) * scale, (topPos(client) + slot.y + 8) * scale};
		});
		context.getInput().setCursorPos(center[0], center[1]);
		context.waitTicks(3);
	}

	private static void shiftSweep(ClientGameTestContext context, int from, int to) {
		context.getInput().holdShift();
		context.runOnClient(client -> {
			MouseButtonEvent click = slotEvent(client, from);
			MouseButtonEvent drag = slotEvent(client, to);
			client.gui.screen().mouseClicked(click, false);
			client.gui.screen().mouseDragged(drag, drag.x() - click.x(), drag.y() - click.y());
			client.gui.screen().mouseReleased(drag);
		});
		context.getInput().releaseShift();
	}

	private static MouseButtonEvent slotEvent(Minecraft client, int slotIndex) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
		Slot slot = screen.getMenu().getSlot(slotIndex);
		return new MouseButtonEvent(leftPos(client) + slot.x + 8, topPos(client) + slot.y + 8,
				new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_SHIFT));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
