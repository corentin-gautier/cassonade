package com.cassonade.test;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import com.cassonade.client.FurnaceExperienceTooltip;
import com.cassonade.client.InfoPanel;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.trader.TraderTimePanel;
import com.cassonade.mixin.AbstractFurnaceBlockEntityAccessor;

public class InfoPanelsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("xp set @a 30 levels");

			placements();
			anvil(context, world);
			grindstone(context, world);
			cooking(context, world, "furnace", Blocks.FURNACE, Items.RAW_IRON, FurnaceScreen.class, true);
			cooking(context, world, "blast-furnace", Blocks.BLAST_FURNACE, Items.RAW_IRON, BlastFurnaceScreen.class, false);
			cooking(context, world, "smoker", Blocks.SMOKER, Items.BEEF, SmokerScreen.class, false);
			brewing(context, world);
			wanderingTrader(context, world);
		}
	}

	private static void anvil(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos pos = player.blockPosition().east(2);
			player.level().setBlockAndUpdate(pos, Blocks.ANVIL.defaultBlockState());
			player.openMenu(Blocks.ANVIL.defaultBlockState().getMenuProvider(player.level(), pos));
		});
		context.waitForScreen(AnvilScreen.class);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			AnvilMenu menu = (AnvilMenu) player.containerMenu;
			ItemStack sword = new ItemStack(Items.IRON_SWORD);
			sword.setDamageValue(100);
			menu.getSlot(0).set(sword);
			menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
			menu.createResult();
			menu.broadcastChanges();
		});
		context.waitTicks(2);
		check(context.computeOnClient(client -> ((AnvilScreen) client.gui.screen()).getMenu().getCost() > 0
						&& ((AnvilScreen) client.gui.screen()).getMenu().getSlot(2).hasItem()),
				"the anvil panel has a repair cost");
		capture(context, "anvil");
		context.setScreen(() -> null);
	}

	private static void grindstone(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("item replace entity @a inventory.3 with diamond_sword[enchantments={sharpness:3}]");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos pos = player.blockPosition().east(2);
			player.level().setBlockAndUpdate(pos, Blocks.GRINDSTONE.defaultBlockState());
			MenuProvider provider = Blocks.GRINDSTONE.defaultBlockState().getMenuProvider(player.level(), pos);
			player.openMenu(provider);
		});
		context.waitForScreen(GrindstoneScreen.class);
		clickSlot(context, 6);
		clickSlot(context, GrindstoneMenu.INPUT_SLOT);
		world.getConnection().waitForServerboundPackets();
		world.getConnection().waitForClientboundPackets();
		check(context.computeOnClient(client -> ((GrindstoneScreen) client.gui.screen()).getMenu()
						.getSlot(GrindstoneMenu.RESULT_SLOT).hasItem()),
				"the grindstone panel has an experience range to show");
		capture(context, "grindstone");
		context.setScreen(() -> null);
	}

	private static void cooking(ClientGameTestContext context, TestSingleplayerContext world, String name, Block block,
			Item ingredient, Class<? extends Screen> screenType, boolean showExperience) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos pos = player.blockPosition().east(2);
			player.level().setBlockAndUpdate(pos, block.defaultBlockState());
			AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) player.level().getBlockEntity(pos);
			Container inventory = furnace;
			inventory.setItem(AbstractFurnaceMenu.INGREDIENT_SLOT, new ItemStack(ingredient, 3));
			inventory.setItem(AbstractFurnaceMenu.FUEL_SLOT, new ItemStack(Items.COAL, 2));
			if (showExperience) {
				((AbstractFurnaceBlockEntityAccessor) furnace).cassonade$getRecipesUsed()
						.addTo(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("iron_ingot_from_smelting_raw_iron")), 3);
			}
			player.openMenu((MenuProvider) furnace);
		});
		context.waitForScreen(screenType);
		context.waitTicks(25);
		check(context.computeOnClient(client -> screenType.isInstance(client.gui.screen())
						&& client.player.containerMenu instanceof AbstractFurnaceMenu menu
						&& menu.isLit() && menu.getSlot(AbstractFurnaceMenu.INGREDIENT_SLOT).hasItem()),
				"the " + name + " panel has active cooking information");
		if (showExperience) {
			int tenths = context.computeOnClient(client ->
					FurnaceExperienceTooltip.tenths(client.player.containerMenu.containerId));
			check(tenths == 21, "the furnace panel also shows stored experience (" + tenths + " tenths)");
		}
		capture(context, name);
		context.setScreen(() -> null);
	}

	private static void brewing(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos pos = player.blockPosition().east(2);
			player.level().setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
			Container stand = (Container) player.level().getBlockEntity(pos);
			for (int slot = 0; slot < 3; slot++) {
				stand.setItem(slot, PotionContents.createItemStack(Items.POTION, Potions.WATER));
			}
			stand.setItem(3, new ItemStack(Items.NETHER_WART));
			stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
			player.openMenu((MenuProvider) stand);
		});
		context.waitForScreen(BrewingStandScreen.class);
		context.waitTicks(25);
		check(context.computeOnClient(client -> {
			BrewingStandMenu menu = ((BrewingStandScreen) client.gui.screen()).getMenu();
			return menu.getBrewingTicks() > 0 && menu.getFuel() > 0;
		}), "the brewing stand panel has an active batch and remaining fuel");
		capture(context, "brewing");
		context.setScreen(() -> null);
	}

	private static void wanderingTrader(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			WanderingTrader trader = EntityTypes.WANDERING_TRADER.create(server.overworld(), EntitySpawnReason.COMMAND);
			trader.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
			trader.setNoAi(true);
			trader.setDespawnDelay(12000);
			server.overworld().addFreshEntity(trader);
			trader.setTradingPlayer(player);
			trader.openTradingScreen(player, trader.getDisplayName(), 0);
		});
		context.waitForScreen(MerchantScreen.class);
		context.waitTicks(45);
		String time = context.computeOnClient(client -> TraderTimePanel.shown());
		check(time.startsWith("Leaves in 10:"), "the wandering trader panel shows its departure time (" + time + ")");
		capture(context, "wandering-trader");
		context.setScreen(() -> null);
	}

	private static void placements() {
		int[] icon = InfoPanel.iconPosition(new InfoPanel.Window(200, 100, 176, 166, 576, 324));
		check(icon[0] + 11 < 200 + 176 && icon[1] > 100 && icon[0] > 200 + 176 - 30 && icon[1] < 100 + 20,
				"the icon sits inside the window's top right corner");
	}

	private static void checkOnScreen(ClientGameTestContext context, String name) {
		boolean onScreen = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
			int[] icon = InfoPanel.iconPosition(new InfoPanel.Window(accessor.cassonade$getLeftPos(),
					accessor.cassonade$getTopPos(), accessor.cassonade$getImageWidth(), accessor.cassonade$getImageHeight(),
					screen.width, screen.height));
			return icon[0] >= 0 && icon[1] >= 0 && icon[0] + 11 <= screen.width && icon[1] + 11 <= screen.height;
		});
		check(onScreen, "the " + name + " icon is inside the screen");
	}

	private static void clickSlot(ClientGameTestContext context, int slotIndex) {
		context.runOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			var slot = screen.getMenu().getSlot(slotIndex);
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
			double x = accessor.cassonade$getLeftPos() + slot.x + 8;
			double y = accessor.cassonade$getTopPos() + slot.y + 8;
			MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
			MouseButtonEvent event = new MouseButtonEvent(x, y, left);
			screen.mouseClicked(event, false);
			screen.mouseReleased(event);
		});
		context.waitTicks(2);
	}

	/** Captures the screen with the cursor on the info icon, so that its tooltip shows, or away from the slots. */
	private static void capture(ClientGameTestContext context, String name) {
		checkOnScreen(context, name);
		double[] icon = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
			int[] position = InfoPanel.iconPosition(new InfoPanel.Window(accessor.cassonade$getLeftPos(),
					accessor.cassonade$getTopPos(), accessor.cassonade$getImageWidth(), accessor.cassonade$getImageHeight(),
					screen.width, screen.height));
			double scale = client.getWindow().getGuiScale();
			return new double[] {(position[0] + 5) * scale, (position[1] + 5) * scale};
		});
		context.getInput().setCursorPos(icon[0], icon[1]);
		context.waitTicks(1);
		context.runOnClient(client -> {
			AchievementBanner.clear();
			client.gui.toastManager().clear();
		});
		context.waitTicks(1);
		context.takeScreenshot("cassonade-info-panel-" + name);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
