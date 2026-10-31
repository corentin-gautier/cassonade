package com.cassonade.client.map;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelData;

import com.cassonade.Cassonade;
import com.cassonade.client.DescriptionText;
import com.cassonade.client.LabeledKeys;
import com.cassonade.client.CassonadeClient;
import com.cassonade.compass.Coordinates;
import com.cassonade.compass.MapMarkerPayload;
import com.cassonade.compass.Marker;
import com.cassonade.compass.PlayerMarkers;

/**
 * Every chunk the player explored in the current dimension, with the player and its markers, over
 * the whole screen in the minimap's frame. The title, the position under the cursor and the list of
 * shortcuts are laid over the map in small frames, the markers' names under their banner.
 * Dragging moves the map, the wheel zooms around the cursor and the zoom keys around the center, space
 * centers it back on the player. A right click places a marker, asking for its name and color, or
 * removes the one clicked. The world's spawn chunk is outlined, and the lodestones the player's compasses point
 * to are marked; these and the biome under the cursor are named on hover.
 */
public class WorldMapScreen extends Screen {
	private static final Identifier FRAME = FramedPanel.FRAME;
	// Fog of war over the unexplored chunks, a tile per chunk; see tools/gui_textures.py
	private static final Identifier FOG = Cassonade.id("map/fog");
	/** The fog's size, see its .mcmeta. */
	private static final int FOG_TILE = 64;
	private static final int BORDER = FramedPanel.BORDER;
	/** Between the labels and the map's frame, and between their frame and their text. */
	private static final int LABEL_MARGIN = 2;
	private static final int LABEL_PADDING = 3;
	/** Between the shortcuts' keys and what they do. */
	private static final int HELP_COLUMN_GAP = 8;
	private static final List<String> HELP_ACTIONS = List.of("move", "zoom", "biome", "marker", "center", "close");
	/** GUI pixels per block, from a region per 64 pixels to 8 pixels per block. */
	private static final float MIN_ZOOM = 1 / 8f;
	private static final float MAX_ZOOM = 8;
	/** From this zoom out, regions are drawn in low resolution: too many can show to hold them in full. */
	private static final float OVERVIEW_ZOOM = 1 / 4f;
	/** How close to a marker, in GUI pixels, the cursor points at it. */
	private static final int MARKER_RADIUS = 7;
	private static final float BANNER_SCALE = 1;
	private static final float PLAYER_SCALE = 2;
	private static final int SWATCH_SIZE = 10;
	private static final int NAME_BOX_WIDTH = SWATCH_SIZE * 16;
	private static final int NAME_BOX_HEIGHT = 18;
	/** The name box and the color swatches below it. */
	private static final int NAMING_HEIGHT = NAME_BOX_HEIGHT + 2 + SWATCH_SIZE;
	/** The spawn chunk's outline: the theme's paper, over a dark line so it reads on any terrain. */
	private static final int SPAWN_COLOR = 0xFFF0E1BD;
	private static final int SPAWN_SHADOW = 0xFF1C1109;
	/** Below this many GUI pixels, the outline keeps this size around the chunk's middle. */
	private static final int SPAWN_MIN_SIZE = 6;
	private static final long MARKER_FEEDBACK_MILLIS = 500;
	private static final int MARKER_FEEDBACK_RADIUS = 6;

	private record MarkerFeedback(int x, int z, long startedAt) {
	}

	private double centerX;
	private double centerZ;
	private float zoom = 1;
	/** Shown while naming a marker about to be placed at (pendingX, pendingZ). */
	private EditBox nameBox;
	private int pendingX;
	private int pendingZ;
	private DyeColor pendingColor;
	/** Set when a zoom key was pressed, so the + or - it may type next does not zoom a second time. */
	private boolean zoomedByKey;
	private @Nullable MarkerFeedback markerFeedback;
	/** The shortcuts in the bottom left corner: each key, and what it does on the same line. */
	private List<FormattedCharSequence> helpKeys = List.of();
	private List<FormattedCharSequence> helpActions = List.of();
	/** Measured once: the mouse's position is checked against it several times a frame. */
	private int helpKeysWidth;
	private int helpWidth;
	/** Coordinates clicked in the chat (ChatCoordinates): the map opens there, naming a marker. */
	private @Nullable BlockPos placeAt;

	public WorldMapScreen() {
		super(Component.translatable("gui.cassonade.world_map"));
	}

	/** The map centered on the spot, its name box open to place a marker there. */
	public static WorldMapScreen placingAt(int x, int z) {
		WorldMapScreen screen = new WorldMapScreen();
		screen.placeAt = new BlockPos(x, 0, z);
		return screen;
	}

	@Override
	protected void init() {
		helpKeys = HELP_ACTIONS.stream().map(action -> Component.translatable("gui.cassonade.world_map.help." + action + ".key",
				LabeledKeys.printedName(CassonadeClient.OPEN_WORLD_MAP), LabeledKeys.printedName(minecraft.options.keyInventory)).getVisualOrderText()).toList();
		helpActions = HELP_ACTIONS.stream().map(action -> Component.translatable("gui.cassonade.world_map.help." + action).getVisualOrderText()).toList();
		helpKeysWidth = labelWidth(helpKeys);
		helpWidth = helpKeysWidth + HELP_COLUMN_GAP + labelWidth(helpActions) - 2 * (BORDER + LABEL_PADDING);
		centerOnPlayer();
		nameBox = null;
		if (placeAt != null) {
			centerX = placeAt.getX() + 0.5;
			centerZ = placeAt.getZ() + 0.5;
			startNaming(placeAt.getX(), placeAt.getZ(), (mapLeft() + mapRight()) / 2.0, (mapTop() + mapBottom()) / 2.0);
			placeAt = null;
		}
	}

	private void centerOnPlayer() {
		centerX = minecraft.player.getX();
		centerZ = minecraft.player.getZ();
	}

	private int mapLeft() {
		return BORDER;
	}

	private int mapTop() {
		return BORDER;
	}

	private int mapRight() {
		return width - BORDER;
	}

	private int mapBottom() {
		return height - BORDER;
	}

	private int helpWidth() {
		return helpWidth;
	}

	private int helpTop() {
		return height - BORDER - LABEL_MARGIN - labelHeight(helpKeys.size());
	}

	private float screenX(double blockX) {
		return (float) ((mapLeft() + mapRight()) / 2f + (blockX - centerX) * zoom);
	}

	private float screenY(double blockZ) {
		return (float) ((mapTop() + mapBottom()) / 2f + (blockZ - centerZ) * zoom);
	}

	private double blockX(double screenX) {
		return centerX + (screenX - (mapLeft() + mapRight()) / 2f) / zoom;
	}

	private double blockZ(double screenY) {
		return centerZ + (screenY - (mapTop() + mapBottom()) / 2f) / zoom;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		LocalPlayer player = minecraft.player;

		graphics.enableScissor(mapLeft(), mapTop(), mapRight(), mapBottom());
		extractRegions(graphics);
		spawnChunk().ifPresent(box -> {
			graphics.outline(box[0] - 1, box[1] - 1, box[2] - box[0] + 2, box[3] - box[1] + 2, SPAWN_SHADOW);
			graphics.outline(box[0], box[1], box[2] - box[0], box[3] - box[1], SPAWN_COLOR);
		});
		for (Marker marker : PlayerMarkers.of(player).markers()) {
			if (marker.pos().dimension() == player.level().dimension()) {
				BlockPos pos = marker.pos().pos();
				float x = screenX(pos.getX() + 0.5);
				float y = screenY(pos.getZ() + 0.5);
				float half = MapDecorations.banner(graphics, marker.color(), x, y, BANNER_SCALE);
				if (!marker.name().isEmpty()) {
					graphics.centeredText(font, marker.name(), Mth.floor(x), Mth.ceil(y + half) + 2, 0xFFFFFFFF);
				}
			}
		}
		for (LodestoneMarkers.Lodestone lodestone : LodestoneMarkers.here(player)) {
			MapDecorations.lodestone(graphics, screenX(lodestone.pos().getX() + 0.5), screenY(lodestone.pos().getZ() + 0.5), BANNER_SCALE);
		}
		DeathMarker.here(player).ifPresent(pos -> MapDecorations.death(graphics, screenX(pos.getX() + 0.5), screenY(pos.getZ() + 0.5), BANNER_SCALE));
		if (nameBox != null) {
			MapDecorations.banner(graphics, pendingColor, screenX(pendingX + 0.5), screenY(pendingZ + 0.5), BANNER_SCALE);
		}
		MapDecorations.player(graphics, screenX(player.getX()), screenY(player.getZ()), player.getYRot(a), PLAYER_SCALE);
		extractMarkerFeedback(graphics);
		graphics.disableScissor();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, 0, 0, width, height);

		// Title at the top, position under the cursor in the top left corner
		int inset = BORDER + LABEL_MARGIN;
		extractLabel(graphics, List.of(title.getVisualOrderText()), width / 2, inset, 0, 0xFFFFFFFF);
		if (isOnMap(mouseX, mouseY)) {
			Component position = Component.literal(Coordinates.of(Mth.floor(blockX(mouseX)), Mth.floor(blockZ(mouseY))));
			List<FormattedCharSequence> line = List.of(position.getVisualOrderText());
			extractLabel(graphics, line, inset + labelWidth(line) / 2, inset, 0, 0xFFFFFFFF);
		}
		extractHelp(graphics);

		// Widgets last, so the name box is drawn over the map
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (nameBox != null) {
			extractSwatches(graphics);
		} else {
			Optional<Marker> hovered = markerAt(mouseX, mouseY);
			hovered.ifPresent(marker -> graphics.setTooltipForNextFrame(font, List.of(
					marker.name().isEmpty() ? Component.translatable("gui.cassonade.marker.unnamed") : Component.literal(marker.name()),
					DescriptionText.of(Component.literal(Coordinates.of(marker.pos().pos()))),
					DescriptionText.of(Component.translatable("gui.cassonade.marker.remove_hint"))),
					Optional.empty(), mouseX, mouseY));
			Optional<LodestoneMarkers.Lodestone> lodestone = hovered.isPresent() || !isOnMap(mouseX, mouseY) ? Optional.empty()
					: LodestoneMarkers.here(minecraft.player).stream().filter(at -> Math.abs(screenX(at.pos().getX() + 0.5) - mouseX) <= MARKER_RADIUS
							&& Math.abs(screenY(at.pos().getZ() + 0.5) - mouseY) <= MARKER_RADIUS).findFirst();
			lodestone.ifPresent(at -> graphics.setTooltipForNextFrame(font, List.of(at.name(),
					DescriptionText.of(Component.literal(Coordinates.of(at.pos())))), Optional.empty(), mouseX, mouseY));
			boolean spawnHovered = isOnMap(mouseX, mouseY) && spawnChunk().filter(box -> mouseX >= box[0] && mouseX < box[2]
					&& mouseY >= box[1] && mouseY < box[3]).isPresent();
			if (hovered.isEmpty() && lodestone.isEmpty() && spawnHovered) {
				graphics.setTooltipForNextFrame(font, List.of(Component.translatable("gui.cassonade.world_spawn"),
						DescriptionText.of(Component.literal(Coordinates.of(minecraft.level.getRespawnData().pos())))),
						Optional.empty(), mouseX, mouseY);
			}
			if (minecraft.hasShiftDown() && hovered.isEmpty() && lodestone.isEmpty() && isOnMap(mouseX, mouseY) && !spawnHovered) {
				Identifier biome = WorldMap.biomeAt(minecraft.level.dimension(), Mth.floor(blockX(mouseX)), Mth.floor(blockZ(mouseY)));
				if (biome != null) {
					graphics.setTooltipForNextFrame(font, List.of(Component.translatable(biome.toLanguageKey("biome"))),
							Optional.empty(), mouseX, mouseY);
				}
			}
		}
	}

	private int labelHeight(int lines) {
		return 2 * (BORDER + LABEL_PADDING) + lines * font.lineHeight - 1;
	}

	private int labelWidth(List<FormattedCharSequence> lines) {
		return lines.stream().mapToInt(font::width).max().orElse(0) + 2 * (BORDER + LABEL_PADDING);
	}

	/** Lines of text in a framed panel centered on x, its top at y, as wide as the text or {@code width}. */
	private void extractLabel(GuiGraphicsExtractor graphics, List<FormattedCharSequence> lines, int x, int y, int width, int color) {
		int panelWidth = Math.max(width, labelWidth(lines));
		int left = x - panelWidth / 2;
		FramedPanel.draw(graphics, left, y, panelWidth, labelHeight(lines.size()));
		int lineY = y + BORDER + LABEL_PADDING;
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, left + (panelWidth - font.width(line)) / 2, lineY, color);
			lineY += font.lineHeight;
		}
	}

	/** The shortcuts in two columns, in a framed panel in the bottom left corner. */
	private void extractHelp(GuiGraphicsExtractor graphics) {
		int left = BORDER + LABEL_MARGIN;
		FramedPanel.draw(graphics, left, helpTop(), helpWidth(), labelHeight(helpKeys.size()));
		int keyX = left + BORDER + LABEL_PADDING;
		int actionX = keyX + helpKeysWidth - 2 * (BORDER + LABEL_PADDING) + HELP_COLUMN_GAP;
		int lineY = helpTop() + BORDER + LABEL_PADDING;
		for (int i = 0; i < helpKeys.size(); i++) {
			graphics.text(font, helpKeys.get(i), keyX, lineY, 0xFFFFFFFF);
			graphics.text(font, helpActions.get(i), actionX, lineY, 0xFFC8C8C8);
			lineY += font.lineHeight;
		}
	}

	/** The 16 dye colors under the name box, the picked one framed in white. */
	private void extractSwatches(GuiGraphicsExtractor graphics) {
		int top = swatchTop();
		graphics.fill(nameBox.getX() - 1, top - 1, nameBox.getX() + NAME_BOX_WIDTH + 1, top + SWATCH_SIZE + 1, 0xFF000000);
		for (int i = 0; i < PlayerMarkers.COLORS.size(); i++) {
			DyeColor color = PlayerMarkers.COLORS.get(i);
			int left = nameBox.getX() + i * SWATCH_SIZE;
			if (color == pendingColor) {
				graphics.fill(left, top, left + SWATCH_SIZE, top + SWATCH_SIZE, 0xFFFFFFFF);
			}
			graphics.fill(left + 1, top + 1, left + SWATCH_SIZE - 1, top + SWATCH_SIZE - 1, 0xFF000000 | color.getTextureDiffuseColor());
		}
	}

	private void extractMarkerFeedback(GuiGraphicsExtractor graphics) {
		if (markerFeedback == null) {
			return;
		}
		float progress = Mth.clamp((Util.getMillis() - markerFeedback.startedAt()) / (float) MARKER_FEEDBACK_MILLIS, 0, 1);
		if (progress >= 1) {
			return;
		}
		float eased = 1 - (1 - progress) * (1 - progress);
		int radius = MARKER_FEEDBACK_RADIUS + Math.round(8 * eased);
		int alpha = Math.round(200 * (1 - progress));
		int x = Mth.floor(screenX(markerFeedback.x() + 0.5));
		int y = Mth.floor(screenY(markerFeedback.z() + 0.5));
		graphics.outline(x - radius - 1, y - radius - 1, 2 * radius + 3, 2 * radius + 3, alpha << 24 | SPAWN_SHADOW & 0xFFFFFF);
		graphics.outline(x - radius, y - radius, 2 * radius + 1, 2 * radius + 1, alpha << 24 | SPAWN_COLOR & 0xFFFFFF);
	}

	private void showMarkerFeedback(int x, int z) {
		markerFeedback = new MarkerFeedback(x, z, Util.getMillis());
	}

	private int swatchTop() {
		return nameBox.getY() + NAME_BOX_HEIGHT + 2;
	}

	/** The color whose swatch is at (x, y), if any. */
	private Optional<DyeColor> swatchAt(double x, double y) {
		int index = Mth.floor((x - nameBox.getX()) / SWATCH_SIZE);
		if (y < swatchTop() || y >= swatchTop() + SWATCH_SIZE || index < 0 || index >= PlayerMarkers.COLORS.size()) {
			return Optional.empty();
		}
		return Optional.of(PlayerMarkers.COLORS.get(index));
	}

	/**
	 * The chunk where players without a bed of their own come back to life, as {left, top, right, bottom} on
	 * the screen, when it is in this dimension. The game gives every client the world's spawn point.
	 */
	private Optional<int[]> spawnChunk() {
		LevelData.RespawnData spawn = minecraft.level.getRespawnData();
		if (spawn.dimension() != minecraft.level.dimension()) {
			return Optional.empty();
		}
		ChunkPos chunk = ChunkPos.containing(spawn.pos());
		int left = Mth.floor(screenX(chunk.getMinBlockX()));
		int top = Mth.floor(screenY(chunk.getMinBlockZ()));
		int right = Mth.ceil(screenX(chunk.getMaxBlockX() + 1));
		int bottom = Mth.ceil(screenY(chunk.getMaxBlockZ() + 1));
		if (right - left < SPAWN_MIN_SIZE) {
			int middleX = (left + right) / 2;
			int middleY = (top + bottom) / 2;
			left = middleX - SPAWN_MIN_SIZE / 2;
			top = middleY - SPAWN_MIN_SIZE / 2;
			right = left + SPAWN_MIN_SIZE;
			bottom = top + SPAWN_MIN_SIZE;
		}
		return Optional.of(new int[] {left, top, right, bottom});
	}

	/** The marker drawn under the cursor. */
	private Optional<Marker> markerAt(double x, double y) {
		if (!isOnMap(x, y)) {
			return Optional.empty();
		}
		return PlayerMarkers.of(minecraft.player).markers().stream()
				.filter(marker -> marker.pos().dimension() == minecraft.level.dimension()
						&& Math.abs(screenX(marker.pos().pos().getX() + 0.5) - x) <= MARKER_RADIUS
						&& Math.abs(screenY(marker.pos().pos().getZ() + 0.5) - y) <= MARKER_RADIUS)
				.findFirst();
	}

	/**
	 * Draws the visible regions, scaled around the map center, over fog fixed to the world, a pixel per block, so
	 * the fog keeps the map's scale and its grain moves with the map. Its tile spans four chunks: on a single one, the
	 * grain repeated closely enough to show a pattern.
	 */
	private void extractRegions(GuiGraphicsExtractor graphics) {
		int minRegionX = Math.floorDiv(Mth.floor(blockX(mapLeft())), MapRegion.BLOCKS);
		int maxRegionX = Math.floorDiv(Mth.floor(blockX(mapRight())), MapRegion.BLOCKS);
		int minRegionZ = Math.floorDiv(Mth.floor(blockZ(mapTop())), MapRegion.BLOCKS);
		int maxRegionZ = Math.floorDiv(Mth.floor(blockZ(mapBottom())), MapRegion.BLOCKS);

		graphics.pose().pushMatrix();
		graphics.pose().translate(screenX(0), screenY(0));
		graphics.pose().scale(zoom, zoom);
		// Zoomed out, a tile spans more of the world, so there are never more tiles than at zoom 1
		int fogScale = Math.max(1, Math.round(1 / zoom));
		int span = FOG_TILE * fogScale;
		int fogLeft = Math.floorDiv(Mth.floor(blockX(mapLeft())), span);
		int fogTop = Math.floorDiv(Mth.floor(blockZ(mapTop())), span);
		int fogRight = Math.floorDiv(Mth.floor(blockX(mapRight())), span) + 1;
		int fogBottom = Math.floorDiv(Mth.floor(blockZ(mapBottom())), span) + 1;
		graphics.pose().pushMatrix();
		graphics.pose().scale(fogScale, fogScale);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOG, fogLeft * FOG_TILE, fogTop * FOG_TILE, (fogRight - fogLeft) * FOG_TILE, (fogBottom - fogTop) * FOG_TILE);
		graphics.pose().popMatrix();
		if (zoom <= OVERVIEW_ZOOM) {
			WorldMap.startOverviewFrame(minecraft.level.dimension());
			for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
				for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
					Identifier overview = WorldMap.overview(minecraft.level.dimension(), regionX, regionZ);
					if (overview != null) {
						graphics.blit(RenderPipelines.GUI_TEXTURED, overview, regionX * MapRegion.BLOCKS, regionZ * MapRegion.BLOCKS, 0, 0,
								MapRegion.BLOCKS, MapRegion.BLOCKS, MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE);
					}
				}
			}
			graphics.pose().popMatrix();
			return;
		}
		for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
			for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
				MapRegion region = WorldMap.region(minecraft.level.dimension(), regionX, regionZ, false);
				if (region != null) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, region.texture(), regionX * MapRegion.BLOCKS, regionZ * MapRegion.BLOCKS,
							0, 0, MapRegion.BLOCKS, MapRegion.BLOCKS, MapRegion.BLOCKS, MapRegion.BLOCKS);
				}
			}
		}
		graphics.pose().popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (nameBox != null) {
			Optional<DyeColor> swatch = swatchAt(event.x(), event.y());
			if (swatch.isPresent()) {
				pendingColor = swatch.get();
				return true;
			}
			// Clicking away from the name box gives up the marker
			if (!nameBox.isMouseOver(event.x(), event.y())) {
				stopNaming();
				return true;
			}
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT || !isOnMap(event.x(), event.y())) {
			return super.mouseClicked(event, doubleClick);
		}

		// The red X where the player died is put away by a right click, like a marker removed
		Optional<BlockPos> deathMarker = DeathMarker.here(minecraft.player)
				.filter(pos -> Math.abs(screenX(pos.getX() + 0.5) - event.x()) <= MARKER_RADIUS
						&& Math.abs(screenY(pos.getZ() + 0.5) - event.y()) <= MARKER_RADIUS);
		if (deathMarker.isPresent()) {
			BlockPos pos = deathMarker.get();
			showMarkerFeedback(pos.getX(), pos.getZ());
			DeathMarker.dismiss(minecraft.player);
			return true;
		}
		Optional<Marker> clicked = markerAt(event.x(), event.y());
		if (clicked.isPresent()) {
			BlockPos pos = clicked.get().pos().pos();
			showMarkerFeedback(pos.getX(), pos.getZ());
			ClientPlayNetworking.send(new MapMarkerPayload(pos.getX(), pos.getZ(), "", clicked.get().color(), true));
		} else {
			startNaming(Mth.floor(blockX(event.x())), Mth.floor(blockZ(event.y())), event.x(), event.y());
		}
		return true;
	}

	/**
	 * Opens the name box and color swatches under the clicked point; Enter places the marker, Escape
	 * gives it up. The color suggested is the next one in turn.
	 */
	private void startNaming(int blockX, int blockZ, double x, double y) {
		pendingX = blockX;
		pendingZ = blockZ;
		pendingColor = PlayerMarkers.of(minecraft.player).nextColor();
		int boxX = Mth.clamp((int) x - NAME_BOX_WIDTH / 2, mapLeft() + 2, mapRight() - NAME_BOX_WIDTH - 2);
		int boxY = Mth.clamp((int) y + 10, mapTop() + 2, mapBottom() - NAMING_HEIGHT - 2);
		nameBox = new EditBox(font, boxX, boxY, NAME_BOX_WIDTH, NAME_BOX_HEIGHT, Component.translatable("gui.cassonade.marker.name"));
		nameBox.setMaxLength(Marker.MAX_NAME_LENGTH);
		nameBox.setHint(Component.translatable("gui.cassonade.marker.name_hint"));
		addRenderableWidget(nameBox);
		setFocused(nameBox);
	}

	private void placePendingMarker() {
		ClientPlayNetworking.send(new MapMarkerPayload(pendingX, pendingZ, nameBox.getValue(), pendingColor, false));
		showMarkerFeedback(pendingX, pendingZ);
		stopNaming();
	}

	private void stopNaming() {
		removeWidget(nameBox);
		nameBox = null;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || nameBox != null) {
			return false;
		}
		centerX -= dx / zoom;
		centerZ -= dy / zoom;
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY == 0) {
			return false;
		}
		zoom(x, y, scrollY > 0);
		return true;
	}

	/**
	 * Typed + and - zoom too, whatever the keys bound: = is + without shift. A zoom key that typed the
	 * character has already zoomed.
	 */
	@Override
	public boolean charTyped(CharacterEvent event) {
		boolean alreadyZoomed = zoomedByKey;
		zoomedByKey = false;
		if (nameBox != null) {
			return super.charTyped(event);
		}
		if (alreadyZoomed && (event.codepoint() == '+' || event.codepoint() == '=' || event.codepoint() == '-')) {
			return true;
		}
		switch (event.codepoint()) {
			case '+', '=' -> zoom((mapLeft() + mapRight()) / 2.0, (mapTop() + mapBottom()) / 2.0, true);
			case '-' -> zoom((mapLeft() + mapRight()) / 2.0, (mapTop() + mapBottom()) / 2.0, false);
			default -> {
				return super.charTyped(event);
			}
		}
		return true;
	}

	/** Zooms in or out, keeping the block at (x, y) on the screen in place. */
	private void zoom(double x, double y, boolean in) {
		double blockX = blockX(x);
		double blockZ = blockZ(y);
		zoom = Mth.clamp(in ? zoom * 2 : zoom / 2, MIN_ZOOM, MAX_ZOOM);
		centerX = blockX - (x - (mapLeft() + mapRight()) / 2f) / zoom;
		centerZ = blockZ - (y - (mapTop() + mapBottom()) / 2f) / zoom;
	}

	private boolean isOnMap(double x, double y) {
		boolean underHelp = x < BORDER + LABEL_MARGIN + helpWidth() && y >= helpTop();
		return x >= mapLeft() && x < mapRight() && y >= mapTop() && y < mapBottom() && !underHelp;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (nameBox != null) {
			if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
				placePendingMarker();
				return true;
			}
			if (event.isEscape()) {
				stopNaming();
				return true;
			}
			return super.keyPressed(event);
		}
		zoomedByKey = false;
		// Its key closes it, like the inventory's, which closes every screen
		if (CassonadeClient.OPEN_WORLD_MAP.matches(event) || minecraft.options.keyInventory.matches(event)) {
			onClose();
			return true;
		}
		if (CassonadeClient.ZOOM_IN.matches(event) || CassonadeClient.ZOOM_OUT.matches(event)) {
			zoom((mapLeft() + mapRight()) / 2.0, (mapTop() + mapBottom()) / 2.0, CassonadeClient.ZOOM_IN.matches(event));
			zoomedByKey = true;
			return true;
		}
		if (event.key() == InputConstants.KEY_SPACE) {
			centerOnPlayer();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		WorldMap.releaseTextures();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
