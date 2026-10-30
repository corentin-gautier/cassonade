package com.cassonade.test;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;

public class BookEditClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("item replace entity @a hotbar.0 with writable_book[writable_book_content={pages:[\"a\",\"b\"]}]");
			context.waitTicks(2);
			context.setScreen(() -> {
				Minecraft client = Minecraft.getInstance();
				client.player.getInventory().setSelectedSlot(0);
				ItemStack book = client.player.getMainHandItem();
				return new BookEditScreen(client.player, book, InteractionHand.MAIN_HAND, book.get(DataComponents.WRITABLE_BOOK_CONTENT));
			});
			context.waitForScreen(BookEditScreen.class);
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-book-edit");

			// A page inserted before the first one, written on right away: x, a, b
			press(context, "+");
			ScreenInput.typeOnScreen(context, "x");
			context.waitTicks(1);
			// The next page deleted, leaving x, b
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(0, 1073741902, 0)));
			press(context, "-");
			press(context, "gui.done");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ItemStack saved = server.getPlayerList().getPlayers().getFirst().getMainHandItem();
				check(saved.is(Items.WRITABLE_BOOK), "the book in hand");
				List<String> pages = saved.get(DataComponents.WRITABLE_BOOK_CONTENT).pages().stream().map(Filterable::raw).toList();
				check(pages.equals(List.of("x", "b")), "the pages inserted and deleted (" + pages + ")");
			});
		}
	}

	private static void press(ClientGameTestContext context, String message) {
		context.runOnClient(client -> {
			Component target = message.length() == 1 ? Component.literal(message) : Component.translatable(message);
			Button button = client.gui.screen().children().stream()
					.filter(Button.class::isInstance).map(Button.class::cast)
					.filter(candidate -> candidate.getMessage().getString().equals(target.getString()))
					.findFirst().orElseThrow();
			MouseButtonEvent event = new MouseButtonEvent(button.getX() + 4, button.getY() + 4, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
			// Held on to: "Done" closes the screen before the button is released
			var screen = client.gui.screen();
			screen.mouseClicked(event, false);
			screen.mouseReleased(event);
		});
		context.waitTicks(2);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
