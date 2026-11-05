package com.cassonade.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

/**
 * {@code /dev edit}: a tavern is built before the player with its structure block set to save it; saved, it is written into
 * the project's resources (a copy here) with its zombie village's copy, the keg looked into in the meantime given its loot back;
 * without its jigsaw, it is not saved.
 */
public class TemplateEditingClientGameTest implements FabricClientGameTest {
	private static final String TEMPLATE = "village/taiga/houses/taiga_temple_1";
	private static final String ZOMBIE = "village/taiga/zombie/houses/taiga_temple_1";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		Path resources;
		try {
			// The templates as the game has them, in a copy the test may overwrite
			resources = Files.createTempDirectory("cassonade-templates");
			for (String name : new String[] {TEMPLATE, ZOMBIE}) {
				Path file = file(resources, name);
				Files.createDirectories(file.getParent());
				try (InputStream in = TemplateEditingClientGameTest.class.getResourceAsStream("/data/minecraft/structure/" + name + ".nbt")) {
					Files.copy(in, file);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		CompoundTag original = read(file(resources, TEMPLATE));
		byte[] originalBytes = bytes(file(resources, TEMPLATE));
		String before = System.getProperty("cassonade.resources");
		System.setProperty("cassonade.resources", resources.toString());
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(state -> state.setAllowCommands(true)).create()) {
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("time set 6000");
			int ground = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0));
			world.getServer().runCommand("tp @a 0.5 " + ground + " 0.5 facing 10 " + (ground + 1) + " 0.5");
			context.waitTicks(20);
			context.runOnClient(client -> client.player.connection.sendCommand("dev edit minecraft:" + TEMPLATE));
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "template-editing");

			// Facing east, the structure block at the box's near corner; a keg looked into in the meantime
			BlockPos corner = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				StructureBlockEntity block = null;
				for (BlockPos pos : BlockPos.betweenClosed(-2, ground - 1, -12, 14, ground + 1, 12)) {
					if (level.getBlockEntity(pos) instanceof StructureBlockEntity found) {
						block = found;
					}
				}
				if (block == null) {
					return null;
				}
				BlockPos origin = block.getBlockPos().offset(block.getStructurePos());
				for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(block.getStructureSize()))) {
					if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel && barrel.getLootTable() != null) {
						barrel.unpackLootTable(null);
						barrel.setItem(0, new ItemStack(Items.BREAD));
						break;
					}
				}
				return block.getBlockPos().immutable();
			});
			check(corner != null, "dev edit sets a structure block");
			// As a player does: its screen opened, its Save button pressed
			context.runOnClient(client -> {
				StructureBlockEntity block = (StructureBlockEntity) client.level.getBlockEntity(corner);
				check(block != null && block.getMode() == StructureMode.SAVE, "the client sees the structure block in save mode: " + (block == null ? null : block.getMode()));
				client.gui.setScreen(new StructureBlockEditScreen(block));
			});
			context.waitTicks(5);
			Screenshots.take(context, "template-editing-screen");
			context.runOnClient(client -> client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
					.filter(button -> button.getMessage().getString().equals(Component.translatable("structure_block.button.save").getString()))
					.findFirst().orElseThrow(() -> new AssertionError("no Save button")).onPress(null));
			context.waitTicks(20);
			context.runOnClient(client -> client.gui.setScreen(null));
			check(!Arrays.equals(bytes(file(resources, TEMPLATE)), originalBytes), "the Save button writes the template into the project");

			// The same kegs as before, the one looked into given back its loot rather than the bread put in it
			List<String> kegs = kegs(read(file(resources, TEMPLATE)));
			System.out.println("Template editing: " + kegs);
			check(kegs.equals(kegs(original)), "the kegs keep their loot: " + kegs + ", was " + kegs(original));
			check(Arrays.equals(bytes(file(resources, TEMPLATE)), bytes(file(resources, ZOMBIE))), "the zombie village's copy is written along");
			check(world.getServer().computeOnServer(server -> !Files.exists(server.getWorldPath(LevelResource.GENERATED_DIR).resolve("minecraft/structure/" + TEMPLATE + ".nbt"))), "nothing is saved in the world");

			// Without its jigsaw, the tavern could no longer join its street: not saved
			byte[] kept = bytes(file(resources, TEMPLATE));
			String refused = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				for (BlockPos pos : BlockPos.betweenClosed(-2, ground - 1, -12, 14, ground + 1, 12)) {
					if (level.getBlockEntity(pos) instanceof StructureBlockEntity block) {
						BlockPos origin = block.getBlockPos().offset(block.getStructurePos());
						for (BlockPos inside : BlockPos.betweenClosed(origin, origin.offset(block.getStructureSize()))) {
							if (level.getBlockState(inside).is(Blocks.JIGSAW)) {
								level.setBlock(inside, Blocks.COBBLESTONE.defaultBlockState(), 2);
							}
						}
						return block.saveStructure() ? "saved" : null;
					}
				}
				return "no structure block";
			});
			check(refused == null, "a tavern without its jigsaw is not saved: " + refused);
			check(Arrays.equals(kept, bytes(file(resources, TEMPLATE))), "the template is left as it was");

			// All of them at once, each with its structure block: the taiga's only here, the zombie copy following it
			world.getServer().runCommand("tp @a 0.5 " + ground + " 40.5 facing 10 " + (ground + 1) + " 40.5");
			context.waitTicks(5);
			context.runOnClient(client -> client.player.connection.sendCommand("dev edit all"));
			context.waitTicks(20);
			long blocks = world.getServer().computeOnServer(server -> BlockPos.betweenClosedStream(4, ground, 38, 6, ground, 60)
					.filter(pos -> server.overworld().getBlockEntity(pos) instanceof StructureBlockEntity).count());
			check(blocks == 1, "dev edit all builds each template with its structure block: " + blocks);
		} finally {
			if (before == null) {
				System.clearProperty("cassonade.resources");
			} else {
				System.setProperty("cassonade.resources", before);
			}
		}
	}

	/** Each barrel's loot table, empty for none, in the template's order; failing on one holding items. */
	private static List<String> kegs(CompoundTag tag) {
		List<String> kegs = new ArrayList<>();
		ListTag palette = tag.getListOrEmpty("palette");
		for (Tag entry : tag.getListOrEmpty("blocks")) {
			CompoundTag block = (CompoundTag) entry;
			String id = palette.getCompoundOrEmpty(block.getIntOr("state", -1)).getStringOr("id", palette.getCompoundOrEmpty(block.getIntOr("state", -1)).getStringOr("Name", ""));
			if (id.equals("minecraft:barrel")) {
				CompoundTag nbt = block.getCompoundOrEmpty("nbt");
				check(nbt.getListOrEmpty("Items").isEmpty(), "a keg keeps no items of its own: " + nbt);
				kegs.add(nbt.getStringOr("LootTable", ""));
			}
		}
		kegs.sort(null);
		return kegs;
	}

	private static Path file(Path resources, String name) {
		return resources.resolve("data/minecraft/structure/" + name + ".nbt");
	}

	private static CompoundTag read(Path file) {
		try {
			return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static byte[] bytes(Path file) {
		try {
			return Files.readAllBytes(file);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
