package com.cassonade.test;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.SelectableSlotContainer;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.spyglass.GlancePanel;

public class BookshelfClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			BlockPos pos = context.computeOnClient(client -> client.player.blockPosition().east(3));
			List<BlockHitResult> hits = shelfHits(pos);
			SelectableSlotContainer shelf = (SelectableSlotContainer) Blocks.CHISELED_BOOKSHELF;
			BlockHitResult selectedHit = hits.getFirst();
			int selectedSlot = shelf.getHitSlot(selectedHit, Direction.NORTH).orElseThrow();
			BlockHitResult emptyHit = hits.stream()
					.filter(hit -> shelf.getHitSlot(hit, Direction.NORTH).orElseThrow() != selectedSlot)
					.findFirst().orElseThrow();

			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				level.setBlockAndUpdate(pos, Blocks.CHISELED_BOOKSHELF.defaultBlockState()
						.setValue(ChiseledBookShelfBlock.FACING, Direction.NORTH));
				ChiseledBookShelfBlockEntity bookshelf = (ChiseledBookShelfBlockEntity) level.getBlockEntity(pos);
				ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
				book.set(DataComponents.WRITTEN_BOOK_CONTENT,
						new WrittenBookContent(Filterable.passThrough("Field Notes"), "Author", 0, List.of(), false));
				bookshelf.setItem(selectedSlot, book);
			});
			context.waitTicks(3);

			List<Component> title = context.computeOnClient(client -> GlancePanel.readBookAt(client.level, selectedHit));
			check(title.size() == 2 && title.get(0).getString().equals("Field Notes")
					&& title.get(1).getString().contains("Author"), "the aimed-at book's title and author are shown");
			check(context.computeOnClient(client -> GlancePanel.readBookAt(client.level, emptyHit).isEmpty()),
					"an empty bookshelf slot shows no title");
		}
	}

	private static List<BlockHitResult> shelfHits(BlockPos pos) {
		List<BlockHitResult> hits = new ArrayList<>();
		for (double x : new double[] {0.2, 0.5, 0.8}) {
			for (double y : new double[] {0.25, 0.75}) {
				hits.add(new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ()),
						Direction.NORTH, pos, false));
			}
		}
		return hits;
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
