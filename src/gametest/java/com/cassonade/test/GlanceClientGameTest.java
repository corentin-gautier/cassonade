package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;

import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.spyglass.GlancePanel;

/**
 * What the crosshair reads at arm's length, with no spyglass: a note block's note, a renamed chest's or Shulker
 * box's name, a playing jukebox's song, and with the right item in hand, whether copper is waxed or a block powered.
 */
public class GlanceClientGameTest implements FabricClientGameTest {
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
			world.getServer().runCommand("time set noon");
			BlockPos feet = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
			// A note block's note, read with no spyglass, just by looking at it
			world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(feet.east(),
					Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, 6)));
			look(world, feet, 50);
			context.waitTicks(5);
			List<String> note = context.computeOnClient(client -> GlancePanel.shown());
			check(note.equals(List.of("Note Block", "C4, tuned 6 of 24")), "the note block's note (" + note + ")");
			Screenshots.take(context, "cassonade-note-block");

			// A chest's name, sent by the server and read the same way, from either half of a double chest
			// Facing north, a left half's other half is east of it: the name is on the half beyond the one looked at
			BlockPos near = feet.east();
			world.getServer().runCommand("setblock " + near.getX() + " " + near.getY() + " " + near.getZ() + " minecraft:chest[type=left,facing=north]");
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ()
					+ " minecraft:chest[type=right,facing=north]{CustomName:\"Ores\"}");
			look(world, feet, 50);
			context.waitTicks(5);
			List<String> chest = context.computeOnClient(client -> GlancePanel.shown());
			String at = near.getX() + " " + near.getY() + " " + near.getZ();
			check(chest.equals(List.of("Ores")), "the chest's name (" + chest + ")");
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ() + " minecraft:air");

			// A Shulker box's name, the same way
			world.getServer().runCommand("setblock " + at + " minecraft:shulker_box{CustomName:\"Tools\"}");
			context.waitTicks(5);
			List<String> shulker = context.computeOnClient(client -> GlancePanel.shown());
			check(shulker.equals(List.of("Tools")), "the Shulker box's name (" + shulker + ")");

			// A playing jukebox's song, learned from the event that started it
			world.getServer().runCommand("setblock " + at + " minecraft:jukebox");
			world.getServer().runCommand("item replace block " + at + " container.0 with minecraft:music_disc_cat");
			context.waitTicks(5);
			List<String> jukebox = context.computeOnClient(client -> GlancePanel.shown());
			check(jukebox.equals(List.of("Jukebox", "C418 - cat")), "the jukebox's song (" + jukebox + ")");
			world.getServer().runCommand("setblock " + at + " minecraft:air");

			// With a honeycomb in hand, whether copper is waxed; nothing without it
			world.getServer().runCommand("setblock " + at + " minecraft:exposed_copper");
			context.waitTicks(5);
			check(context.computeOnClient(client -> GlancePanel.shown()).isEmpty(), "nothing read on copper with an empty hand");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with honeycomb");
			context.waitTicks(5);
			List<String> copper = context.computeOnClient(client -> GlancePanel.shown());
			check(copper.equals(List.of("Exposed Copper", "Not waxed")), "the copper is not waxed (" + copper + ")");
			world.getServer().runCommand("setblock " + at + " minecraft:waxed_exposed_copper");
			context.waitTicks(5);
			List<String> waxed = context.computeOnClient(client -> GlancePanel.shown());
			check(waxed.equals(List.of("Waxed Exposed Copper", "Waxed")), "the copper is waxed (" + waxed + ")");

			// With redstone dust in hand, a wire's power, here next to a block of redstone
			world.getServer().runCommand("setblock " + at + " minecraft:redstone_wire");
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ() + " minecraft:redstone_block");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with redstone");
			context.waitTicks(5);
			List<String> wire = context.computeOnClient(client -> GlancePanel.shown());
			check(wire.equals(List.of("Redstone Wire", "Power: 15 of 15")), "the wire's power (" + wire + ")");

			// Any other block: whether it is powered, here a stone block beside a block of redstone, then without it
			world.getServer().runCommand("setblock " + at + " minecraft:stone");
			context.waitTicks(5);
			List<String> powered = context.computeOnClient(client -> GlancePanel.shown());
			check(powered.equals(List.of("Stone", "Powered: 15 of 15")), "the stone is powered (" + powered + ")");
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ() + " minecraft:air");
			context.waitTicks(5);
			List<String> unpowered = context.computeOnClient(client -> GlancePanel.shown());
			check(unpowered.equals(List.of("Stone", "Not powered")), "the stone is not powered (" + unpowered + ")");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		}
	}

	/** On the middle of the block, looking east and down by the angle. */
	private static void look(TestSingleplayerContext world, BlockPos feet, float down) {
		world.getServer().runCommand("tp @a " + (feet.getX() + 0.5) + " " + feet.getY() + " " + (feet.getZ() + 0.5) + " -90 " + down);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
