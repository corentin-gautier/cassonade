package com.cassonade.test;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.cassonade.Cassonade;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * What item tooltips add: an enchantment's description, always on a book, on gear while Shift is held, in
 * both languages for every enchantment; the anvil's penalty of an item worked at it.
 */
public class TooltipsClientGameTest implements FabricClientGameTest {
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

			// An enchantment's description: always on a book, on gear only while Shift is held
			context.setScreen(() -> null);
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a inventory.0 with enchanted_book[stored_enchantments={\"minecraft:sharpness\":2}]");
			world.getServer().runCommand("item replace entity @a inventory.1 with diamond_sword[enchantments={\"minecraft:sharpness\":2}]");
			context.waitTicks(2);
			String sharpness = context.computeOnClient(client -> Language.getInstance().getOrDefault("gui.cassonade.enchantment.minecraft.sharpness"));
			check(context.computeOnClient(client -> tooltip(client, 9).contains(sharpness)), "an enchanted book describes its enchantment");
			check(!context.computeOnClient(client -> tooltip(client, 10).contains(sharpness)), "an enchanted sword keeps its tooltip short");
			context.getInput().holdShift();
			context.waitTicks(1);
			check(context.computeOnClient(client -> tooltip(client, 10).contains(sharpness)), "an enchanted sword describes its enchantment while Shift is held");
			context.getInput().releaseShift();

			// Every enchantment of the game has its description, in both languages
			String missing = context.computeOnClient(client -> {
				JsonObject french = frenchLanguage(client);
				return client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElementIds()
						.map(key -> "gui.cassonade.enchantment." + key.identifier().getNamespace() + "." + key.identifier().getPath())
						.filter(key -> !Language.getInstance().has(key) || !french.has(key))
						.collect(Collectors.joining(", "));
			});
			check(missing.isEmpty(), "every enchantment has a description in English and French (missing: " + missing + ")");

			// The anvil's penalty, once an item has one
			world.getServer().runCommand("item replace entity @a inventory.3 with iron_pickaxe[repair_cost=7]");
			context.waitTicks(2);
			String penalty = context.computeOnClient(client -> Component.translatable("gui.cassonade.repair_cost", 7).getString());
			check(context.computeOnClient(client -> tooltip(client, 12).contains(penalty)), "a pickaxe worked at the anvil shows its penalty");
			check(!context.computeOnClient(client -> tooltip(client, 10).contains(penalty)), "a sword never worked at the anvil shows none");

			// How the descriptions look: a book with two, wrapped, and the sword with Shift held
			world.getServer().runCommand("item replace entity @a inventory.2 with enchanted_book[stored_enchantments={\"minecraft:mending\":1,\"minecraft:unbreaking\":3}]");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			hover(context, 11);
			Screenshots.take(context, "cassonade-enchantment-book");
			hover(context, 10);
			context.getInput().holdShift();
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-enchantment-sword");
			context.getInput().releaseShift();
		}
	}

	/** The tooltip of the player's inventory slot (9 to 35), as plain text. */
	private static String tooltip(Minecraft client, int slot) {
		ItemStack stack = client.player.getInventory().getItem(slot);
		return stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream()
				.map(Component::getString).collect(Collectors.joining("\n"));
	}

	private static JsonObject frenchLanguage(Minecraft client) {
		Identifier file = Identifier.fromNamespaceAndPath(Cassonade.MOD_ID, "lang/fr_fr.json");
		try (Reader reader = client.getResourceManager().getResourceOrThrow(file).openAsReader()) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static int leftPos(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).cassonade$getLeftPos();
	}

	private static int topPos(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).cassonade$getTopPos();
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

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
