package com.vanillasugar.client.map;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.compass.MapMarkerPayload;
import com.vanillasugar.compass.Marker;
import com.vanillasugar.compass.PlayerMarkers;

/**
 * Every chunk the player explored in the current dimension, with the player and its markers.
 * Dragging moves the map, the wheel zooms around the cursor and the zoom keys around the center, space
 * centers it back on the player. A right click places a marker, asking for its name and color, or
 * removes the one clicked.
 */
public class WorldMapScreen extends Screen {
	private static final Identifier FRAME = VanillaSugar.id("hud/minimap_frame");
	private static final int MARGIN = 8;
	private static final int BORDER = 4;
	private static final int TOP = 20;
	private static final int BOTTOM = 24;
	private static final int BACKGROUND_COLOR = 0xFF101010;
	/** GUI pixels per block, from a region per 64 pixels to 8 pixels per block. */
	private static final float MIN_ZOOM = 1 / 8f;
	private static final float MAX_ZOOM = 8;
	/** How close to a marker, in GUI pixels, the cursor points at it. */
	private static final int MARKER_RADIUS = 7;
	private static final float BANNER_SCALE = 1.5f;
	private static final int SWATCH_SIZE = 10;
	private static final int NAME_BOX_WIDTH = SWATCH_SIZE * 16;
	private static final int NAME_BOX_HEIGHT = 18;
	/** The name box and the color swatches below it. */
	private static final int NAMING_HEIGHT = NAME_BOX_HEIGHT + 2 + SWATCH_SIZE;

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

	public WorldMapScreen() {
		super(Component.translatable("gui.vanillasugar.world_map"));
	}

	@Override
	protected void init() {
		centerOnPlayer();
		nameBox = null;
	}

	private void centerOnPlayer() {
		centerX = minecraft.player.getX();
		centerZ = minecraft.player.getZ();
	}

	private int mapLeft() {
		return MARGIN + BORDER;
	}

	private int mapTop() {
		return TOP + BORDER;
	}

	private int mapRight() {
		return width - MARGIN - BORDER;
	}

	private int mapBottom() {
		return height - BOTTOM - BORDER;
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

		graphics.centeredText(font, title, width / 2, (TOP - font.lineHeight) / 2 + 1, 0xFFFFFFFF);
		graphics.fill(mapLeft(), mapTop(), mapRight(), mapBottom(), BACKGROUND_COLOR);
		graphics.enableScissor(mapLeft(), mapTop(), mapRight(), mapBottom());
		extractRegions(graphics);
		for (Marker marker : PlayerMarkers.of(player).markers()) {
			if (marker.pos().dimension() == player.level().dimension()) {
				BlockPos pos = marker.pos().pos();
				MapDecorations.banner(graphics, marker.color(), screenX(pos.getX() + 0.5), screenY(pos.getZ() + 0.5), BANNER_SCALE);
			}
		}
		if (nameBox != null) {
			MapDecorations.banner(graphics, pendingColor, screenX(pendingX + 0.5), screenY(pendingZ + 0.5), BANNER_SCALE);
		}
		MapDecorations.player(graphics, screenX(player.getX()), screenY(player.getZ()), player.getYRot(a));
		graphics.disableScissor();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, MARGIN, TOP, width - 2 * MARGIN, height - TOP - BOTTOM);

		int infoY = height - BOTTOM + (BOTTOM - font.lineHeight) / 2 + 1;
		if (isOnMap(mouseX, mouseY)) {
			String position = Mth.floor(blockX(mouseX)) + ", " + Mth.floor(blockZ(mouseY));
			graphics.text(font, position, MARGIN, infoY, 0xFFFFFFFF);
		}
		Component help = Component.translatable("gui.vanillasugar.world_map.help");
		graphics.text(font, help, width - MARGIN - font.width(help), infoY, 0xFFA0A0A0);

		// Widgets last, so the name box is drawn over the map
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (nameBox != null) {
			extractSwatches(graphics);
		} else {
			markerAt(mouseX, mouseY).ifPresent(marker -> graphics.setTooltipForNextFrame(font, List.of(
					marker.name().isEmpty() ? Component.translatable("gui.vanillasugar.marker.unnamed") : Component.literal(marker.name()),
					Component.literal(marker.pos().pos().toShortString()).withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.vanillasugar.marker.remove_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)),
					Optional.empty(), mouseX, mouseY));
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

	/** Draws the visible regions, scaled around the map center. */
	private void extractRegions(GuiGraphicsExtractor graphics) {
		int minRegionX = Math.floorDiv(Mth.floor(blockX(mapLeft())), MapRegion.BLOCKS);
		int maxRegionX = Math.floorDiv(Mth.floor(blockX(mapRight())), MapRegion.BLOCKS);
		int minRegionZ = Math.floorDiv(Mth.floor(blockZ(mapTop())), MapRegion.BLOCKS);
		int maxRegionZ = Math.floorDiv(Mth.floor(blockZ(mapBottom())), MapRegion.BLOCKS);

		graphics.pose().pushMatrix();
		graphics.pose().translate(screenX(0), screenY(0));
		graphics.pose().scale(zoom, zoom);
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

		Optional<Marker> clicked = markerAt(event.x(), event.y());
		if (clicked.isPresent()) {
			BlockPos pos = clicked.get().pos().pos();
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
		nameBox = new EditBox(font, boxX, boxY, NAME_BOX_WIDTH, NAME_BOX_HEIGHT, Component.translatable("gui.vanillasugar.marker.name"));
		nameBox.setMaxLength(Marker.MAX_NAME_LENGTH);
		nameBox.setHint(Component.translatable("gui.vanillasugar.marker.name_hint"));
		addRenderableWidget(nameBox);
		setFocused(nameBox);
	}

	private void placePendingMarker() {
		ClientPlayNetworking.send(new MapMarkerPayload(pendingX, pendingZ, nameBox.getValue(), pendingColor, false));
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
		return x >= mapLeft() && x < mapRight() && y >= mapTop() && y < mapBottom();
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
		if (VanillaSugarClient.OPEN_WORLD_MAP.matches(event)) {
			onClose();
			return true;
		}
		if (VanillaSugarClient.ZOOM_IN.matches(event) || VanillaSugarClient.ZOOM_OUT.matches(event)) {
			zoom((mapLeft() + mapRight()) / 2.0, (mapTop() + mapBottom()) / 2.0, VanillaSugarClient.ZOOM_IN.matches(event));
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
