package com.cassonade.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.waystone.Waystones;
import com.cassonade.woodcutter.Woodcutter;

/**
 * The website's recipe icons for the blocks among the ingredients (promo/site/assets/items), which have no flat texture:
 * drawn by the game as in the inventory, at its smallest scale, so they match the other items' 16 pixels. Run on demand
 * only, with {@code ./gradlew runClientGameTest -PsiteIcons}, apart from the screenshots: the sculk sensor's and the
 * woodcutter's textures are animated, so each run catches another frame.
 */
public class SiteIconsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		String folder = System.getProperty("cassonade.siteIcons");
		if (folder == null) {
			return;
		}
		// In a world, where items have their components
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			blockIcons(context, Path.of(folder), Items.LIGHTNING_ROD.weathering().unaffected(), Items.SCULK_SENSOR, Woodcutter.ITEM, Waystones.ITEM,
					Items.CHISELED_STONE_BRICKS);
		}
	}

	/** A screen of one plain color, the icons drawn on it in a row, for blockIcons to cut them out. */
	private static final class IconSheet extends Screen {
		static final int KEY = 0xFFFF00FF;
		static final int STEP = 24;
		private final Item[] items;

		IconSheet(Item[] items) {
			super(Component.empty());
			this.items = items;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			graphics.fill(0, 0, width, height, KEY);
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			for (int i = 0; i < items.length; i++) {
				graphics.item(new ItemStack(items[i]), STEP + i * STEP, STEP);
			}
		}
	}

	/** Each item drawn by the game at 16 pixels, cut out of a screenshot, saved as <id>.png with a clear background. */
	private static void blockIcons(ClientGameTestContext context, Path folder, Item... items) {
		int scale = context.computeOnClient(client -> client.options.guiScale().get());
		context.runOnClient(client -> {
			client.options.guiScale().set(1);
			client.resizeGui();
		});
		context.setScreen(() -> new IconSheet(items));
		context.waitTicks(3);
		Path sheet = Screenshots.take(context, "site-icons");
		try (NativeImage image = NativeImage.read(Files.readAllBytes(sheet))) {
			Files.createDirectories(folder);
			for (int i = 0; i < items.length; i++) {
				try (NativeImage icon = new NativeImage(16, 16, true)) {
					for (int y = 0; y < 16; y++) {
						for (int x = 0; x < 16; x++) {
							int color = image.getPixel(IconSheet.STEP + i * IconSheet.STEP + x, IconSheet.STEP + y);
							icon.setPixel(x, y, color == IconSheet.KEY ? 0 : color);
						}
					}
					icon.writeToFile(folder.resolve(BuiltInRegistries.ITEM.getKey(items[i]).getPath() + ".png"));
				}
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		context.setScreen(() -> null);
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
	}
}
