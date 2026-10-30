package com.cassonade.test;

import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;

import com.cassonade.client.FurnaceExperienceTooltip;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.mixin.AbstractFurnaceBlockEntityAccessor;

/**
 * What workstations tell: a fuel's worth in a furnace, the furnace's time left and experience held, the
 * brewing stand's time and fuel, the experience a grindstone gives back.
 */
public class WorkstationsClientGameTest implements FabricClientGameTest {
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
