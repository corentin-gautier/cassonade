package com.cassonade.test;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.BackpackPanel;
import com.cassonade.gear.GearLayout;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/** The inventory's own handling, one case at a time: a shift click dragged right after clicking a tab. */
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

			// The same right after the backpack's tab opens it: both go into the backpack
			context.setScreen(() -> null);
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a inventory.0 with brick 5");
			world.getServer().runCommand("item replace entity @a inventory.1 with clay_ball 5");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.BACKPACK, new ItemStack(CassonadeItems.BACKPACK)));
			});
			context.waitTicks(2);
			context.runOnClient(client -> BackpackPanel.setOpen(false));
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			click(context, client -> leftPos(client) + GearLayout.tabX(1) + GearLayout.TAB_WIDTH / 2,
					client -> topPos(client) + GearLayout.TAB_Y + 8);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "the backpack's tab opens it");
			shiftSweep(context, InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_START + 1);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			String packed = context.computeOnClient(client -> client.player.getInventory().countItem(Items.BRICK) + " / "
					+ client.player.getInventory().countItem(Items.CLAY_BALL));
			check(packed.equals("0 / 0"), "the first shift click dragged after the backpack's tab moves both stacks (" + packed + ")");
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
