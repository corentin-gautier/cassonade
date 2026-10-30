package com.cassonade.test;

import java.util.function.ToIntFunction;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;

/** Mouse and keyboard input sent to the screen open, for the tests acting on windows. */
final class ScreenInput {
	private ScreenInput() {
	}

	/** Left click at a GUI position computed on the client. */
	static void click(ClientGameTestContext context, ToIntFunction<Minecraft> x, ToIntFunction<Minecraft> y) {
		context.runOnClient(client -> {
			MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
			client.gui.screen().mouseClicked(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left), false);
			client.gui.screen().mouseReleased(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left));
		});
		context.waitTicks(2);
	}

	/** Middle click at a GUI position computed on the client. */
	static void middleClick(ClientGameTestContext context, ToIntFunction<Minecraft> x, ToIntFunction<Minecraft> y) {
		context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client),
				new MouseButtonInfo(InputConstants.MOUSE_BUTTON_MIDDLE, 0)), false));
		context.waitTicks(2);
	}

	static void typeOnScreen(ClientGameTestContext context, String text) {
		context.runOnClient(client -> text.codePoints().forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
	}

	static void pressOnScreen(ClientGameTestContext context, int key) {
		context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(key, 0, 0)));
	}

	/** Clears the advancements' banners and the recipes' toasts, which would cover the screens. */
	static void quiet(ClientGameTestContext context) {
		context.runOnClient(client -> {
			AchievementBanner.clear();
			client.gui.toastManager().clear();
		});
		context.waitTicks(1);
	}

	/** A left button event at the center of a slot of the screen open, in GUI pixels. */
	static MouseButtonEvent slotEvent(Minecraft client, int slotIndex, int modifiers) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
		Slot slot = screen.getMenu().getSlot(slotIndex);
		AbstractContainerScreenAccessor position = (AbstractContainerScreenAccessor) screen;
		return new MouseButtonEvent(position.cassonade$getLeftPos() + slot.x + 8, position.cassonade$getTopPos() + slot.y + 8,
				new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, modifiers));
	}

	static void shiftSweep(ClientGameTestContext context, int from, int to, boolean holdButton) {
		context.getInput().holdShift();
		// The test's mouse buttons carry no modifiers, nor drag the cursor: the events go to the screen directly
		context.runOnClient(client -> {
			MouseButtonEvent click = slotEvent(client, from, InputConstants.MOD_SHIFT);
			MouseButtonEvent drag = slotEvent(client, to, InputConstants.MOD_SHIFT);
			client.gui.screen().mouseClicked(click, false);
			if (!holdButton) {
				client.gui.screen().mouseReleased(click);
			}
			client.gui.screen().mouseDragged(drag, drag.x() - click.x(), drag.y() - click.y());
			if (holdButton) {
				client.gui.screen().mouseReleased(drag);
			}
		});
		context.getInput().releaseShift();
		context.waitTicks(2);
	}

	/** A click on a slot of the screen open, the lock key held. */
	static void lockClick(ClientGameTestContext context, int slotIndex) {
		context.getInput().holdKey(CassonadeClient.LOCK_SLOT);
		context.runOnClient(client -> {
			MouseButtonEvent click = slotEvent(client, slotIndex, InputConstants.MOD_ALT);
			client.gui.screen().mouseClicked(click, false);
			client.gui.screen().mouseReleased(click);
		});
		context.getInput().releaseKey(CassonadeClient.LOCK_SLOT);
		context.waitTicks(2);
	}

	static void quickMove(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(
				client.player.inventoryMenu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player));
	}
}
