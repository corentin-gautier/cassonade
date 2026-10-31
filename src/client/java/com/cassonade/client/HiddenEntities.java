package com.cassonade.client;

import org.jspecify.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * The entities the terrain hides. Vanilla only skips those out of the view, drawing a whole cave of mobs behind
 * a mountain. The game already works out which sections the eye can reach to skip their blocks, empty ones
 * included: an entity in none of them cannot be seen, for the same reason as the blocks around it.
 */
public final class HiddenEntities {
	/**
	 * Sodium never feeds vanilla's section graph, which would look as if it reached nothing, and culls entities with
	 * its own, keeping private the sections it cannot tell about: those without a block, whose entities it draws.
	 */
	private static final boolean ENABLED = !FabricLoader.getInstance().isModLoaded("sodium");

	private HiddenEntities() {
	}

	public static boolean isHidden(Entity entity, AABB box) {
		if (!ENABLED || showsThroughTerrain(entity)) {
			return false;
		}
		Minecraft minecraft = Minecraft.getInstance();
		ViewArea area = minecraft.levelRenderer.viewArea();
		if (area == null) {
			return false;
		}
		SectionOcclusionGraph graph = minecraft.levelRenderer.sectionOcclusionGraph();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = SectionPos.blockToSectionCoord(box.minX); x <= SectionPos.blockToSectionCoord(box.maxX); x++) {
			for (int y = SectionPos.blockToSectionCoord(box.minY); y <= SectionPos.blockToSectionCoord(box.maxY); y++) {
				for (int z = SectionPos.blockToSectionCoord(box.minZ); z <= SectionPos.blockToSectionCoord(box.maxZ); z++) {
					SectionRenderDispatcher.@Nullable RenderSection section = area.getRenderSectionAt(pos.set(SectionPos.sectionToBlockCoord(x), SectionPos.sectionToBlockCoord(y), SectionPos.sectionToBlockCoord(z)));
					// Outside the sections (above the build limit): nothing says it is hidden
					if (section == null || graph.getNode(section) != null) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/** Outlines and name tags are drawn through walls, and a leash reaches beyond its entity's sections. */
	private static boolean showsThroughTerrain(Entity entity) {
		return entity instanceof Player || entity.hasCustomName() || Minecraft.getInstance().shouldEntityAppearGlowing(entity)
				|| entity instanceof Leashable leashable && leashable.getLeashHolder() != null;
	}
}
