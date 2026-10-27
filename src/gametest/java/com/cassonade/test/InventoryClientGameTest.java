package com.cassonade.test;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

import com.cassonade.Cassonade;
import com.cassonade.client.PickedUpItems;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.BackpackPanel;
import com.cassonade.gear.GearLayout;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.storage.LockedSlots;

/**
 * The inventory's own handling, one case at a time: a shift click dragged right after clicking a tab, the
 * enchantments' descriptions and the anvil's penalty in tooltips, the list of items picked up, the drop key
 * sparing a locked slot, the fuels' worth in a furnace, the grindstone's experience.
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
			context.takeScreenshot("cassonade-enchantment-book");
			hover(context, 10);
			context.getInput().holdShift();
			context.waitTicks(2);
			context.takeScreenshot("cassonade-enchantment-sword");
			context.getInput().releaseShift();

			// What the player picks up, listed on the right, the same item's pickups added up, gone after a moment
			context.setScreen(() -> null);
			world.getServer().runCommand("execute at @a run summon item ~ ~ ~ {Item:{id:\"minecraft:cobblestone\",count:4}}");
			world.getServer().runCommand("execute at @a run summon item ~ ~ ~ {Item:{id:\"minecraft:dirt\",count:1}}");
			context.waitTicks(10);
			world.getServer().runCommand("execute at @a run summon item ~ ~ ~ {Item:{id:\"minecraft:cobblestone\",count:6}}");
			context.waitTicks(10);
			List<String> picked = context.computeOnClient(client -> PickedUpItems.shown());
			check(picked.equals(List.of("+1 Dirt", "+10 Cobblestone")), "the pickups listed, cobblestone added up and last (" + picked + ")");
			context.takeScreenshot("cassonade-picked-up");
			context.waitTicks(80);
			check(context.computeOnClient(client -> PickedUpItems.shown()).isEmpty(), "the list gone after a moment");

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

			// In a furnace, and only there, a fuel tells how many items it smelts
			world.getServer().runCommand("item replace entity @a inventory.0 with coal 3");
			world.getServer().runCommand("item replace entity @a inventory.1 with oak_planks 3");
			context.waitTicks(2);
			check(!context.computeOnClient(client -> tooltip(client, 9)).contains("smelts"), "no fuel line out of a furnace");
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ furnace");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos furnace = player.blockPosition().east(2);
				player.openMenu((MenuProvider) player.level().getBlockEntity(furnace));
			});
			context.waitForScreen(FurnaceScreen.class);
			String coal = context.computeOnClient(client -> tooltip(client, 9));
			String planks = context.computeOnClient(client -> tooltip(client, 10));
			check(coal.contains("Fuel: smelts 8 items") && planks.contains("Fuel: smelts 1.5 items"),
					"each fuel's items smelted in a furnace (" + coal + " / " + planks + ")");
			context.setScreen(() -> null);

			// Cooking, the furnace tells when it is done and how much more its fuel will cook
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Container furnace = (Container) player.level().getBlockEntity(player.blockPosition().east(2));
				furnace.setItem(AbstractFurnaceMenu.INGREDIENT_SLOT, new ItemStack(Items.RAW_IRON, 4));
				furnace.setItem(AbstractFurnaceMenu.FUEL_SLOT, new ItemStack(Items.COAL, 2));
			});
			context.waitTicks(30);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().east(2)));
			});
			context.waitForScreen(FurnaceScreen.class);
			context.runOnClient(client -> {
				AchievementBanner.clear();
				client.gui.toastManager().clear();
			});
			context.waitTicks(2);
			context.takeScreenshot("cassonade-furnace-times");
			context.setScreen(() -> null);

			// The grindstone tells the experience it gives back before the item is taken
			world.getServer().runCommand("item replace entity @a inventory.0 with diamond_sword[enchantments={sharpness:3,vanishing_curse:1}]");
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ grindstone");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.openMenu(Blocks.GRINDSTONE.defaultBlockState().getMenuProvider(player.level(), player.blockPosition().east(2)));
			});
			context.waitForScreen(GrindstoneScreen.class);
			click(context, client -> (int) slotEvent(client, 3).x(), client -> (int) slotEvent(client, 3).y());
			click(context, client -> (int) slotEvent(client, GrindstoneMenu.INPUT_SLOT).x(), client -> (int) slotEvent(client, GrindstoneMenu.INPUT_SLOT).y());
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> ((GrindstoneScreen) client.gui.screen()).getMenu().getSlot(GrindstoneMenu.RESULT_SLOT).hasItem()),
					"the sword ground in the grindstone");
			context.runOnClient(client -> {
				AchievementBanner.clear();
				client.gui.toastManager().clear();
			});
			context.waitTicks(2);
			context.takeScreenshot("cassonade-grindstone-experience");
			context.setScreen(() -> null);
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
