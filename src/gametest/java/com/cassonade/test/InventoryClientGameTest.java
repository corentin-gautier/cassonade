package com.cassonade.test;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
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
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;

import com.cassonade.Cassonade;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.FurnaceExperienceTooltip;
import com.cassonade.client.NewItems;
import com.cassonade.client.PickedUpItems;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.storage.ContainerSearch;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.BackpackPanel;
import com.cassonade.gear.GearLayout;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.mixin.AbstractFurnaceBlockEntityAccessor;
import com.cassonade.storage.LockedSlots;

/**
 * The inventory's own handling, one case at a time: a shift click dragged right after clicking a tab, the
 * enchantments' descriptions and the anvil's penalty in tooltips, the list of items picked up and their dot, the
 * drop key sparing a locked slot, the rows rotated into the hotbar, the fuels' worth in a furnace, the grindstone's experience.
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
			check(context.computeOnClient(client -> client.gui.screen().children().stream()
					.anyMatch(widget -> widget instanceof ImageButton button && button.getX() == backpackSearchButtonX(client))),
					"the open backpack has a search button");
			check(context.computeOnClient(client -> client.gui.screen().children().stream()
					.filter(EditBox.class::isInstance).map(EditBox.class::cast).anyMatch(search ->
							search.getX() == backpackSearchFieldLeft(client) && search.getY() == backpackSearchFieldTop(client))),
					"the backpack search field is centered above the panel and clear of its strap");
			click(context, client -> backpackSearchButtonX(client) + 5, client -> topPos(client) + 9);
			check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof EditBox),
					"the backpack search button opens and focuses the search field");
			click(context, InventoryClientGameTest::backpackSearchFieldCenterX, InventoryClientGameTest::backpackSearchFieldCenterY);
			check(context.computeOnClient(client -> client.gui.screen() instanceof InventoryScreen && BackpackPanel.isOpen()
					&& client.gui.screen().getFocused() instanceof EditBox),
					"clicking the backpack search field does not activate a widget behind it");
			context.runOnClient(client -> "brick".codePoints()
					.forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				boolean foundBrick = false;
				boolean foundClayBall = false;
				boolean brickDimmed = true;
				boolean clayBallDimmed = false;
				for (int i = GearLayout.BACKPACK_START; i < GearLayout.BACKPACK_END; i++) {
					Slot slot = screen.getMenu().getSlot(i);
					if (slot.getItem().is(Items.BRICK)) {
						foundBrick = true;
						brickDimmed = ContainerSearch.dims(screen, slot);
					} else if (slot.getItem().is(Items.CLAY_BALL)) {
						foundClayBall = true;
						clayBallDimmed = ContainerSearch.dims(screen, slot);
					}
				}
				return foundBrick && foundClayBall && !brickDimmed && clayBallDimmed;
			}), "search matches backpack contents and dims the non-matching stack");
			Screenshots.take(context, "cassonade-backpack-search");
			click(context, InventoryClientGameTest::emptyInventorySlotCenterX, InventoryClientGameTest::emptyInventorySlotCenterY);
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				for (int i = GearLayout.BACKPACK_START; i < GearLayout.BACKPACK_END; i++) {
					if (ContainerSearch.dims(screen, screen.getMenu().getSlot(i))) {
						return false;
					}
				}
				return !(screen.getFocused() instanceof EditBox);
			}), "clicking outside the search field closes it and restores every backpack item");
			click(context, client -> backpackSearchButtonX(client) + 5, client -> topPos(client) + 9);
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				return screen.getFocused() == null && !ContainerSearch.dims(screen, screen.getMenu().getSlot(GearLayout.BACKPACK_START));
			}), "Escape closes and clears backpack search");

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

			// What the player picks up, listed on the right, the same item's pickups added up, gone after a moment
			context.setScreen(() -> null);
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
			// Three raw irons cooked, 0.7 experience each: the furnace holds 2.1
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				((AbstractFurnaceBlockEntityAccessor) player.level().getBlockEntity(player.blockPosition().east(2))).cassonade$getRecipesUsed()
						.addTo(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("iron_ingot_from_smelting_raw_iron")), 3);
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().east(2)));
			});
			context.waitForScreen(FurnaceScreen.class);
			context.waitTicks(15);
			int held = context.computeOnClient(client -> FurnaceExperienceTooltip.tenths(client.player.containerMenu.containerId));
			check(held == 21, "the furnace's experience reaches the open window (" + held + ")");
			context.runOnClient(client -> {
				AchievementBanner.clear();
				client.gui.toastManager().clear();
			});
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-furnace-times");
			context.setScreen(() -> null);

			// Search opens from Ctrl+F or its title button, and dims only storage slots that do not match
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:1},"
					+ "{Slot:1b,id:\"minecraft:stone\",count:1}]}");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().east(2)));
			});
			context.waitForScreen(ContainerScreen.class);
			check(context.computeOnClient(client -> client.gui.screen().children().stream()
					.anyMatch(widget -> widget instanceof ImageButton button && button.getX() == searchButtonX(client))),
					"the chest has a search button");
			context.runOnClient(client -> client.gui.screen().keyPressed(
					new KeyEvent(InputConstants.KEY_F, InputConstants.KEY_F, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER)));
			check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof EditBox), "Ctrl+F opens and focuses the search field");
			check(context.computeOnClient(client -> client.gui.screen().keyPressed(
					new KeyEvent(InputConstants.KEY_E, InputConstants.KEY_E, 0))
					&& client.gui.screen() instanceof ContainerScreen
					&& client.gui.screen().getFocused() instanceof EditBox),
					"the inventory key does not close the screen while the search field has focus");
			context.runOnClient(client -> "diamond".codePoints().forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				return !ContainerSearch.dims(screen, screen.getMenu().getSlot(0)) && ContainerSearch.dims(screen, screen.getMenu().getSlot(1));
			}), "search keeps matching items bright and dims other stacks");
			Screenshots.take(context, "cassonade-container-search");
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				return !ContainerSearch.dims(screen, screen.getMenu().getSlot(0)) && !ContainerSearch.dims(screen, screen.getMenu().getSlot(1));
			}), "closing search clears the query and restores every storage item");
			click(context,
					client -> client.gui.screen().children().stream()
							.filter(widget -> widget instanceof ImageButton button && button.getX() == searchButtonX(client))
							.map(widget -> ((ImageButton) widget).getX() + 5).findFirst().orElseThrow(),
					client -> client.gui.screen().children().stream()
							.filter(widget -> widget instanceof ImageButton button && button.getX() == searchButtonX(client))
							.map(widget -> ((ImageButton) widget).getY() + 5).findFirst().orElseThrow());
			check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof EditBox), "the search button opens the field too");
			context.setScreen(() -> null);

			// Creative search indexes item tooltips asynchronously, then filters its live tab on each typed character
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.setScreen(new CreativeModeInventoryScreen(client.player,
					client.player.connection.enabledFeatures(), false)));
			context.waitForScreen(CreativeModeInventoryScreen.class);
			List<ItemStack> indexed = context.computeOnClient(client -> client.getConnection().searchTrees().creativeNameSearch().search("diamond"));
			check(indexed.stream().anyMatch(stack -> stack.is(Items.DIAMOND)), "the creative item search index returns diamonds");
			context.runOnClient(client -> {
				try {
					Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
					selectTab.setAccessible(true);
					selectTab.invoke(client.gui.screen(), BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.SEARCH));
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			});
			context.runOnClient(client -> "diamond".codePoints()
					.forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
			check(context.computeOnClient(client -> ((CreativeModeInventoryScreen) client.gui.screen()).getMenu().items.stream()
					.anyMatch(stack -> stack.is(Items.DIAMOND))), "typing in the creative search tab filters its items");
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");

			// The brewing stand's own menu syncs the current time and fuelled batches
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ brewing_stand");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Container stand = (Container) player.level().getBlockEntity(player.blockPosition().east(2));
				for (int slot = 0; slot < 3; slot++) {
					stand.setItem(slot, PotionContents.createItemStack(Items.POTION, Potions.WATER));
				}
				stand.setItem(3, new ItemStack(Items.NETHER_WART));
				stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().east(2)));
			});
			context.waitForScreen(BrewingStandScreen.class);
			context.waitTicks(2);
			int[] brewing = context.computeOnClient(client -> {
				BrewingStandMenu menu = ((BrewingStandScreen) client.gui.screen()).getMenu();
				return new int[] {menu.getBrewingTicks(), menu.getFuel()};
			});
			check(brewing[0] > 0 && brewing[1] > 0, "the brewing stand shows an active batch and remaining fuel (" + brewing[0] + " / " + brewing[1] + ")");
			Screenshots.take(context, "cassonade-brewing-times");
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
			Screenshots.take(context, "cassonade-grindstone-experience");
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

	private static int searchButtonX(Minecraft client) {
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) client.gui.screen();
		return accessor.cassonade$getLeftPos() + accessor.cassonade$getImageWidth() - 7 - 11 - 2 * (11 + 2);
	}

	private static int backpackSearchButtonX(Minecraft client) {
		return leftPos(client) + GearLayout.PANEL_X + GearLayout.PANEL_WIDTH - 7 - 11 - 2 * (11 + 2);
	}

	private static int backpackSearchFieldCenterX(Minecraft client) {
		return backpackSearchFieldLeft(client) + 112 / 2;
	}

	private static int backpackSearchFieldCenterY(Minecraft client) {
		return backpackSearchFieldTop(client) + 22 / 2;
	}

	private static int backpackSearchFieldLeft(Minecraft client) {
		return leftPos(client) + GearLayout.PANEL_X + (GearLayout.PANEL_WIDTH - 112) / 2;
	}

	private static int backpackSearchFieldTop(Minecraft client) {
		return topPos(client) - 8 - 22 - 3;
	}

	private static int emptyInventorySlotCenterX(Minecraft client) {
		Slot slot = ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().getSlot(InventoryMenu.INV_SLOT_START);
		return leftPos(client) + slot.x + 8;
	}

	private static int emptyInventorySlotCenterY(Minecraft client) {
		Slot slot = ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().getSlot(InventoryMenu.INV_SLOT_START);
		return topPos(client) + slot.y + 8;
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
