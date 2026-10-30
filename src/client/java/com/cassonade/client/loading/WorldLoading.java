package com.cassonade.client.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import org.jspecify.annotations.Nullable;

import com.cassonade.Cassonade;
import com.cassonade.client.map.FramedPanel;
import com.cassonade.client.map.MapDecorations;
import com.cassonade.client.map.MapRegion;
import com.cassonade.client.map.WorldMap;

/**
 * The screen shown while a world loads, in singleplayer or joining a server: the map of what was explored around the player, its
 * chunks coming into full color as they load; on land never seen, they are inked on the blank paper.
 */
public final class WorldLoading {
	// See tools/loading_textures.py
	private static final Identifier BACKGROUND = Cassonade.id("loading/background");
	private static final Identifier SAVE_ICON = Cassonade.id("loading/save");
	/** Four GUI pixels per pixel of the 16-pixel icon. */
	private static final int SAVE_ICON_SIZE = 64;
	private static final int SAVE_ICON_GAP = 10;
	private static final int PAPER = 0xFFF0E1BD;
	private static final int INK_HATCH = 0xFFB59E70;
	private static final int INK_LAND = 0xFFD8C495;
	private static final int INK_SHORE = 0xFF3F280A;
	private static final int BAR_DARK = 0xFF9B6501;
	private static final int BAR = 0xFFCE941A;
	private static final int BAR_LIGHT = 0xFFF0B935;
	private static final int BAR_INSIDE = 5;
	private static final int TEXT = 0xFFECD9B2;
	private static final int TIP = 0xFFA89A85;
	private static final int GAP = 10;
	private static final int TIP_GAP = 8;
	private static final int TIP_LINES = 2;
	private static final int TIP_WIDTH = 260;
	private static final int MAX_PAPER = 176;
	private static final int MAX_CELL = 8;
	private static final int TIPS = 6;

	private static volatile @Nullable ResourceKey<Level> focusDimension;
	private static volatile @Nullable ChunkPos focusChunk;
	/** Whose focus it is: the last world's must not show while this one starts. */
	private static volatile @Nullable LevelLoadTracker focusOwner;
	private static @Nullable LevelLoadTracker openedFor;
	private static boolean joining;
	private static boolean betweenSteps;
	private static boolean betweenTips;
	private static @Nullable String betweenName;
	private static @Nullable String lastWorldName;
	private static Component lastStatus = Component.empty();
	private static int tip = RandomSource.create().nextInt(TIPS);

	private WorldLoading() {
	}

	/** Where the world loads around, which the server's thread tells the tracker. */
	public static void focus(LevelLoadTracker tracker, ResourceKey<Level> dimension, ChunkPos chunk) {
		focusDimension = dimension;
		focusChunk = chunk;
		focusOwner = tracker;
	}

	/**
	 * Runs what the game does between the steps of opening a world: it shows a progress screen for a frame
	 * while it closes whatever was open, which stays in this screen's frame, with the step's status.
	 */
	public static void between(boolean tips, @Nullable String name, Runnable closing) {
		betweenSteps = true;
		betweenTips = tips;
		betweenName = name;
		try {
			closing.run();
		} finally {
			betweenSteps = false;
		}
	}

	public static boolean isBetweenSteps() {
		return betweenSteps;
	}

	public static void extractBetweenSteps(GuiGraphicsExtractor graphics, Font font, int width, int height) {
		if (betweenName != null) {
			extractSaving(graphics, font, width, height, lastStatus, betweenName);
		} else {
			extractWaiting(graphics, font, width, height, lastStatus, betweenTips);
		}
	}

	/** The world last loaded, whose name stays on the frame while it saves. */
	public static @Nullable String lastWorldName() {
		return lastWorldName;
	}

	/** A connection to a server starts: its first load shows the map, not its later changes of dimension. */
	public static void startJoining() {
		joining = true;
	}

	public static void stopJoining() {
		joining = false;
	}

	/** The first load of a world: a singleplayer one has a chunk view, a server's follows its connection. */
	public static boolean applies(LevelLoadTracker tracker) {
		return tracker.statusView() != null || joining;
	}

	/** The world is loaded: the next one starts over, with another tip. */
	public static void finished() {
		joining = false;
		lastStatus = Component.empty();
		tip = RandomSource.create().nextInt(TIPS);
		WorldMap.releaseTextures();
	}

	public static void extractBackground(GuiGraphicsExtractor graphics, int width, int height) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width, height);
	}

	public static void extract(GuiGraphicsExtractor graphics, Font font, int width, int height, LevelLoadTracker tracker, float progress) {
		Minecraft minecraft = Minecraft.getInstance();
		// The map's folder is known as soon as the server exists, before the player joins
		if (openedFor != tracker && minecraft.getSingleplayerServer() != null) {
			openedFor = tracker;
			WorldMap.open(minecraft);
		}
		String name = null;
		if (minecraft.getSingleplayerServer() != null) {
			name = minecraft.getSingleplayerServer().getWorldData().getLevelName();
			lastWorldName = name;
		} else if (minecraft.getCurrentServer() != null) {
			name = minecraft.getCurrentServer().name;
		}

		ChunkLoadStatusView view = tracker.statusView();
		ResourceKey<Level> dimension = focusOwner == tracker ? focusDimension : null;
		ChunkPos center = focusChunk;
		float done = progress;
		if (view == null) {
			// A server tells nothing of its loading: what shows is the chunks arrived around the player
			LocalPlayer player = minecraft.player;
			ClientLevel level = minecraft.level;
			// The player stands at the origin until the server places them
			if (player != null && level != null && (player.getX() != 0 || player.getZ() != 0)) {
				dimension = level.dimension();
				center = new ChunkPos(SectionPos.blockToSectionCoord(player.getBlockX()), SectionPos.blockToSectionCoord(player.getBlockZ()));
				view = new ArrivedChunks(level, center);
				done = ((ArrivedChunks) view).share();
			} else {
				done = 0;
			}
		}

		Layout layout = Layout.of(font, width, height);
		extractPaper(graphics, layout);
		if (view != null && dimension != null && center != null) {
			extractMap(graphics, layout.left + FramedPanel.BORDER, layout.top + FramedPanel.BORDER, layout.paper, view, dimension, center);
		}
		extractFrame(graphics, font, width, layout, name);
		extractStatus(graphics, font, layout, Component.translatable("gui.cassonade.loading.status"), done, true);
		extractTip(graphics, font, width, layout);
	}

	/** The same screen with the paper still blank, before there is anything to load, so that nothing jumps. */
	public static void extractWaiting(GuiGraphicsExtractor graphics, Font font, int width, int height, Component status, boolean tips) {
		lastStatus = status;
		extractBackground(graphics, width, height);
		Layout layout = Layout.of(font, width, height);
		extractPaper(graphics, layout);
		extractFrame(graphics, font, width, layout, null);
		extractStatus(graphics, font, layout, status, 0, false);
		if (tips) {
			extractTip(graphics, font, width, layout);
		}
	}

	/** A world saving: nothing to show of its progress, which the game does not tell, so a floppy disk over its name. */
	public static void extractSaving(GuiGraphicsExtractor graphics, Font font, int width, int height, Component status, @Nullable String name) {
		lastStatus = status;
		extractBackground(graphics, width, height);
		int labelHeight = font.lineHeight + 2 * FramedPanel.BORDER + 4;
		int top = (height - labelHeight - SAVE_ICON_GAP - SAVE_ICON_SIZE - SAVE_ICON_GAP - font.lineHeight) / 2;
		if (name != null) {
			int labelWidth = font.width(name) + 2 * FramedPanel.BORDER + 6;
			FramedPanel.draw(graphics, (width - labelWidth) / 2, top, labelWidth, labelHeight);
			graphics.text(font, name, (width - font.width(name)) / 2, top + FramedPanel.BORDER + 2, 0xFFFFFFFF);
		}
		int iconTop = top + labelHeight + SAVE_ICON_GAP;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SAVE_ICON, (width - SAVE_ICON_SIZE) / 2, iconTop, SAVE_ICON_SIZE, SAVE_ICON_SIZE);
		graphics.centeredText(font, status, width / 2, iconTop + SAVE_ICON_SIZE + SAVE_ICON_GAP, TEXT);
	}

	/** Where the tip goes, for what takes its place. */
	public static int tipTop(Font font, int height) {
		return Layout.of(font, 0, height).tipTop;
	}

	public static int frameWidth(Font font, int height) {
		return Layout.of(font, 0, height).frame;
	}

	private record Layout(int paper, int frame, int left, int top, int labelHeight, int statusY, int barTop, int barHeight, int tipTop) {
		private static Layout of(Font font, int width, int height) {
			int labelHeight = font.lineHeight + 2 * FramedPanel.BORDER + 4;
			int barHeight = 2 * FramedPanel.BORDER + BAR_INSIDE;
			// Under the map: the status over the bar, then the tip apart, its room kept so that nothing moves with its length
			int below = GAP + font.lineHeight + 2 + barHeight + TIP_GAP + TIP_LINES * font.lineHeight;
			int paper = Math.min(MAX_PAPER, height - labelHeight - below - 12);
			int frame = paper + 2 * FramedPanel.BORDER;
			int top = (height - labelHeight - frame - below) / 2 + labelHeight - 2;
			int statusY = top + frame + GAP;
			int barTop = statusY + font.lineHeight + 2;
			return new Layout(paper, frame, (width - frame) / 2, top, labelHeight, statusY, barTop, barHeight, barTop + barHeight + TIP_GAP);
		}
	}

	private static void extractPaper(GuiGraphicsExtractor graphics, Layout layout) {
		graphics.fill(layout.left, layout.top, layout.left + layout.frame, layout.top + layout.frame, PAPER);
	}

	private static void extractFrame(GuiGraphicsExtractor graphics, Font font, int width, Layout layout, @Nullable String name) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FramedPanel.FRAME, layout.left, layout.top, layout.frame, layout.frame);
		if (name != null) {
			// The world's name on the frame, like the title of the world map
			int labelWidth = font.width(name) + 2 * FramedPanel.BORDER + 6;
			int labelTop = layout.top - layout.labelHeight + 2;
			FramedPanel.draw(graphics, (width - labelWidth) / 2, labelTop, labelWidth, layout.labelHeight);
			graphics.text(font, name, (width - font.width(name)) / 2, labelTop + FramedPanel.BORDER + 2, 0xFFFFFFFF);
		}
	}

	private static void extractStatus(GuiGraphicsExtractor graphics, Font font, Layout layout, Component status, float progress, boolean showPercent) {
		float done = Mth.clamp(progress, 0, 1);
		graphics.text(font, status, layout.left + 1, layout.statusY, TEXT);
		if (showPercent) {
			Component percent = Component.translatable("gui.cassonade.loading.percent", Mth.floor(done * 100));
			graphics.text(font, percent, layout.left + layout.frame - 1 - font.width(percent), layout.statusY, TEXT);
		}
		FramedPanel.draw(graphics, layout.left, layout.barTop, layout.frame, layout.barHeight);
		int insideLeft = layout.left + FramedPanel.BORDER;
		int insideTop = layout.barTop + FramedPanel.BORDER;
		int filled = Mth.floor(done * (layout.frame - 2 * FramedPanel.BORDER));
		if (filled > 0) {
			graphics.fill(insideLeft, insideTop, insideLeft + filled, insideTop + BAR_INSIDE, BAR);
			graphics.fill(insideLeft, insideTop, insideLeft + filled, insideTop + 1, BAR_LIGHT);
			graphics.fill(insideLeft, insideTop + BAR_INSIDE - 1, insideLeft + filled, insideTop + BAR_INSIDE, BAR_DARK);
		}
	}

	private static void extractTip(GuiGraphicsExtractor graphics, Font font, int width, Layout layout) {
		int tipY = layout.tipTop;
		for (FormattedCharSequence line : font.split(Component.translatable("gui.cassonade.loading.tip." + tip), Math.max(layout.frame, Math.min(width - 40, TIP_WIDTH)))) {
			graphics.text(font, line, (width - font.width(line)) / 2, tipY, TIP);
			tipY += font.lineHeight;
		}
	}

	/** The chunks a server has sent around the player, as far as any server sends them. */
	private record ArrivedChunks(ClientLevel level, ChunkPos center) implements ChunkLoadStatusView {
		private static final int RADIUS = 5;

		@Override
		public void moveTo(ResourceKey<Level> dimension, ChunkPos centerChunk) {
		}

		@Override
		public @Nullable ChunkStatus get(int x, int z) {
			return level.hasChunk(center.x() + x - RADIUS, center.z() + z - RADIUS) ? ChunkStatus.FULL : null;
		}

		@Override
		public int radius() {
			return RADIUS;
		}

		private float share() {
			int arrived = 0;
			int side = 2 * RADIUS + 1;
			for (int z = 0; z < side; z++) {
				for (int x = 0; x < side; x++) {
					arrived += get(x, z) == null ? 0 : 1;
				}
			}
			return arrived / (float) (side * side);
		}
	}

	private static void extractMap(GuiGraphicsExtractor graphics, int left, int top, int size, ChunkLoadStatusView view, ResourceKey<Level> dimension, ChunkPos center) {
		int radius = view.radius();
		int cell = Mth.clamp(size / (2 * radius + 1), 1, MAX_CELL);
		int centerX = left + size / 2;
		int centerY = top + size / 2;
		int cells = size / (2 * cell) + 1;
		graphics.enableScissor(left, top, left + size, top + size);

		// Ink first: explored land covers it, and what its region left unexplored shows it
		for (int dz = -radius; dz <= radius; dz++) {
			for (int dx = -radius; dx <= radius; dx++) {
				extractInk(graphics, view, center, radius, dx, dz, centerX - cell / 2 + dx * cell, centerY - cell / 2 + dz * cell, cell);
			}
		}

		WorldMap.startOverviewFrame(dimension);
		int regionChunks = MapRegion.CHUNKS;
		float blockScale = cell / 16f;
		for (int regionZ = Math.floorDiv(center.z() - cells, regionChunks); regionZ <= Math.floorDiv(center.z() + cells, regionChunks); regionZ++) {
			for (int regionX = Math.floorDiv(center.x() - cells, regionChunks); regionX <= Math.floorDiv(center.x() + cells, regionChunks); regionX++) {
				Identifier sepia = WorldMap.sepiaOverview(dimension, regionX, regionZ);
				if (sepia != null) {
					graphics.pose().pushMatrix();
					// The chunk at the center is centered on the paper
					graphics.pose().translate(centerX - cell / 2f + (regionX * regionChunks - center.x()) * cell, centerY - cell / 2f + (regionZ * regionChunks - center.z()) * cell);
					graphics.pose().scale(blockScale, blockScale);
					graphics.blit(RenderPipelines.GUI_TEXTURED, sepia, 0, 0, 0, 0, MapRegion.BLOCKS, MapRegion.BLOCKS,
							MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE);
					graphics.pose().popMatrix();
				}
			}
		}

		// The chunks loaded come into color
		int texels = MapRegion.OVERVIEW_SIZE / regionChunks;
		for (int dz = -radius; dz <= radius; dz++) {
			for (int dx = -radius; dx <= radius; dx++) {
				if (view.get(dx + radius, dz + radius) == ChunkStatus.FULL) {
					int chunkX = center.x() + dx;
					int chunkZ = center.z() + dz;
					Identifier overview = WorldMap.overview(dimension, Math.floorDiv(chunkX, regionChunks), Math.floorDiv(chunkZ, regionChunks));
					if (overview != null) {
						graphics.blit(RenderPipelines.GUI_TEXTURED, overview, centerX - cell / 2 + dx * cell, centerY - cell / 2 + dz * cell,
								Math.floorMod(chunkX, regionChunks) * texels, Math.floorMod(chunkZ, regionChunks) * texels, cell, cell, texels, texels,
								MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE);
					}
				}
			}
		}
		MapDecorations.player(graphics, centerX, centerY, 180);
		graphics.disableScissor();
	}

	/** A chunk being drawn on the paper: hatched while it generates, then land with a wavering inked shore. */
	private static void extractInk(GuiGraphicsExtractor graphics, ChunkLoadStatusView view, ChunkPos center, int radius, int dx, int dz, int x, int y, int cell) {
		ChunkStatus status = view.get(dx + radius, dz + radius);
		if (status == null) {
			return;
		}
		if (status != ChunkStatus.FULL) {
			for (int py = 0; py < cell; py++) {
				for (int px = 0; px < cell; px++) {
					if ((x + px + y + py) % 4 == 0) {
						graphics.fill(x + px, y + py, x + px + 1, y + py + 1, INK_HATCH);
					}
				}
			}
			return;
		}
		boolean west = !isFull(view, radius, dx - 1, dz);
		boolean east = !isFull(view, radius, dx + 1, dz);
		boolean north = !isFull(view, radius, dx, dz - 1);
		boolean south = !isFull(view, radius, dx, dz + 1);
		if (!west && !east && !north && !south) {
			graphics.fill(x, y, x + cell, y + cell, INK_LAND);
			return;
		}
		for (int py = 0; py < cell; py++) {
			for (int px = 0; px < cell; px++) {
				// Measured from the chunk's world position, so the shore does not shimmer from frame to frame
				int worldX = (center.x() + dx) * cell + px;
				int worldY = (center.z() + dz) * cell + py;
				int inside = Integer.MAX_VALUE;
				if (west) inside = Math.min(inside, px - wave(worldY));
				if (east) inside = Math.min(inside, cell - 1 - px - wave(worldY + 7));
				if (north) inside = Math.min(inside, py - wave(worldX + 13));
				if (south) inside = Math.min(inside, cell - 1 - py - wave(worldX + 23));
				if (inside >= 0) {
					graphics.fill(x + px, y + py, x + px + 1, y + py + 1, inside == 0 ? INK_SHORE : INK_LAND);
				}
			}
		}
	}

	private static boolean isFull(ChunkLoadStatusView view, int radius, int dx, int dz) {
		return Math.abs(dx) <= radius && Math.abs(dz) <= radius && view.get(dx + radius, dz + radius) == ChunkStatus.FULL;
	}

	/** How far a shore bites in, from 0 to 2 pixels, changing every other pixel. */
	private static int wave(int along) {
		return Math.floorMod((along >> 1) * 31 ^ (along >> 3) * 17, 3);
	}
}
