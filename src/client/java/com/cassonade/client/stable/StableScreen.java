package com.cassonade.client.stable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ContainerPanel;
import com.cassonade.client.ScrollSounds;
import com.cassonade.client.WorldSelectionScreen;
import com.cassonade.client.map.SmallText;
import com.cassonade.client.spyglass.HorseStats;
import com.cassonade.stable.StableActionPayload;
import com.cassonade.stable.StableListPayload;
import com.cassonade.stable.StableRenamePayload;
import com.cassonade.stable.Stables;

/**
 * A hitching post's screen: the player's horses on the left, each with its head, its name and where it is; the one picked
 * on the right, drawn whole and turning to the mouse, its name to change, its stats as the naturalist's spyglass shows
 * them, and what can be done with it from here: board it, take it out, or call it back. Under the list, the horse
 * standing by that can be registered. The name is kept on picking another horse or leaving, as in a book.
 */
public class StableScreen extends Screen {
	/** Rows in view: the rest of the stable is scrolled to. */
	private static final int ROWS = 5;
	private static final int ROW_HEIGHT = 30;
	private static final int LIST_WIDTH = 132;
	private static final int DETAIL_WIDTH = 150;
	private static final int PADDING = 6;
	/** The window's rounded edge. */
	private static final int BORDER = 4;
	private static final int COLUMN_GAP = 10;
	private static final int PORTRAIT_SIZE = 26;
	/** How large the horse is drawn in its square, and how far moved, so its head and chest fill it. */
	private static final int PORTRAIT_SCALE = 12;
	private static final float PORTRAIT_DROP = 0.4F;
	private static final float PORTRAIT_SHIFT = 0.8F;
	/** From facing the viewer, in degrees. */
	private static final float PORTRAIT_TURN = 50;
	/** Between the portrait and the text. */
	private static final int TEXT_GAP = 6;
	private static final int STATUS_GAP = 3;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int MIN_HANDLE_HEIGHT = 16;
	private static final int VIEW_HEIGHT = 58;
	private static final int NAME_HEIGHT = 16;
	private static final int BUTTON_HEIGHT = 20;
	private static final int STAT_GAP = 4;
	// The theme's colors (tools/gui_theme.py)
	private static final int TEXT = 0xFFE8DABC;
	private static final int FAINT = 0xFFA8957A;
	private static final int OUTLINE = ContainerPanel.SLOT_OUTLINE;
	private static final int FILL = ContainerPanel.SLOT_FILL;
	private static final int HOVER = 0xFFE8DABC;
	private static final int SELECT = 0xFF7CA72E;
	private static final int HERE = 0xFF9FD07A;

	/** A horse of the list, made on the client from what the server saved, to be drawn and read; never added to the world. */
	private record Shown(StableListPayload.Horse horse, @Nullable AbstractHorse entity) {
	}

	private StableListPayload list;
	private final List<Shown> shown = new ArrayList<>();
	private @Nullable UUID selected;
	private String name = "";
	private int scroll;
	private boolean dragging;

	private StableScreen(StableListPayload list) {
		super(Component.translatable("block.cassonade.hitching_post"));
		this.list = list;
		read(list);
	}

	/** Opens the screen, or brings the one open up to date, after a horse was boarded, taken out or registered. */
	public static void receive(Minecraft minecraft, StableListPayload list) {
		if (minecraft.gui.screen() instanceof StableScreen screen && screen.list.post().equals(list.post())) {
			screen.list = list;
			screen.read(list);
			screen.rebuildWidgets();
		} else {
			minecraft.gui.setScreen(new StableScreen(list));
		}
	}

	private void read(StableListPayload list) {
		Minecraft minecraft = Minecraft.getInstance();
		shown.clear();
		for (StableListPayload.Horse horse : list.horses()) {
			shown.add(new Shown(horse, make(minecraft, horse)));
		}
		if (selected == null || shown.stream().noneMatch(entry -> entry.horse().id().equals(selected))) {
			select(shown.isEmpty() ? null : shown.getFirst());
		} else {
			name = selectedShown().horse().name();
		}
	}

	private static @Nullable AbstractHorse make(Minecraft minecraft, StableListPayload.Horse horse) {
		CompoundTag tag = horse.horse().copyTagWithoutId();
		tag.remove("Passengers");
		tag.remove("leash");
		Entity entity = EntityType.loadEntityRecursive(horse.horse().type(), tag, minecraft.level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
		if (!(entity instanceof AbstractHorse made)) {
			return null;
		}
		// The game reads a mob's attributes only on the server: its speed, jump and health, read here for the stats
		Tag attributes = tag.get("attributes");
		if (attributes != null) {
			AttributeInstance.Packed.LIST_CODEC.parse(minecraft.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), attributes)
					.ifSuccess(made.getAttributes()::apply);
		}
		// Rendering reads its network id; a negative one, which no entity of the world ever has
		made.setId(-1 - minecraft.level.getRandom().nextInt(Integer.MAX_VALUE));
		return made;
	}

	/**
	 * Follows the horses in the world while the screen is open: one walking away can no longer be boarded, one coming
	 * up can. The server checks it all again.
	 */
	@Override
	public void tick() {
		super.tick();
		boolean changed = false;
		for (int i = 0; i < shown.size(); i++) {
			StableListPayload.Horse horse = shown.get(i).horse();
			if (horse.status() == StableListPayload.Status.BOARDED) {
				continue;
			}
			Entity entity = minecraft.level.getEntity(horse.entity());
			StableListPayload.Status status = entity != null && entity.isAlive()
					&& entity.position().closerThan(Vec3.atBottomCenterOf(list.post()), Stables.NEAR)
					? StableListPayload.Status.HERE : StableListPayload.Status.AWAY;
			if (status != horse.status()) {
				shown.set(i, new Shown(new StableListPayload.Horse(horse.id(), horse.entity(), horse.name(), status, horse.horse()), shown.get(i).entity()));
				changed |= horse.id().equals(selected);
			}
		}
		// Only the button of the horse picked depends on it
		if (changed) {
			rebuildWidgets();
		}
	}

	private void select(@Nullable Shown entry) {
		selected = entry == null ? null : entry.horse().id();
		name = entry == null ? "" : entry.horse().name();
	}

	private @Nullable Shown selectedShown() {
		return shown.stream().filter(entry -> entry.horse().id().equals(selected)).findFirst().orElse(null);
	}

	private int panelWidth() {
		return 2 * (BORDER + PADDING) + LIST_WIDTH + COLUMN_GAP + DETAIL_WIDTH;
	}

	private int listHeight() {
		return ROWS * ROW_HEIGHT;
	}

	/** How many rows the list scrolls: public for the tests. */
	public int maxScroll() {
		return Math.max(0, shown.size() - ROWS);
	}

	/** Short of the scrollbar when the list needs one. */
	private int rowWidth() {
		return LIST_WIDTH - (maxScroll() > 0 ? WorldSelectionScreen.SCROLLBAR_SPACE : 0);
	}

	private int scrollbarX() {
		return listLeft() + LIST_WIDTH - SCROLLBAR_WIDTH;
	}

	private int grooveBottom() {
		return listTop() + listHeight() - 2;
	}

	private int handleHeight() {
		int groove = grooveBottom() - listTop();
		return Math.min(groove, Math.max(MIN_HANDLE_HEIGHT, groove * ROWS / shown.size()));
	}

	private boolean isOverScrollbar(double x, double y) {
		return maxScroll() > 0 && x >= scrollbarX() && x < scrollbarX() + SCROLLBAR_WIDTH && y >= listTop() && y < grooveBottom();
	}

	private int panelHeight() {
		return 2 * (BORDER + PADDING) + font.lineHeight + PADDING + listHeight() + PADDING + BUTTON_HEIGHT;
	}

	private int panelLeft() {
		return (width - panelWidth()) / 2;
	}

	private int panelTop() {
		return (height - panelHeight()) / 2;
	}

	private int listLeft() {
		return panelLeft() + BORDER + PADDING;
	}

	private int listTop() {
		return panelTop() + BORDER + PADDING + font.lineHeight + PADDING;
	}

	private int detailLeft() {
		return listLeft() + LIST_WIDTH + COLUMN_GAP;
	}

	private int nameTop() {
		return listTop() + VIEW_HEIGHT + 2;
	}

	private int statsTop() {
		return nameTop() + NAME_HEIGHT + 6;
	}

	private int bottomTop() {
		return listTop() + listHeight() + PADDING;
	}

	@Override
	protected void init() {
		Shown entry = selectedShown();
		if (entry != null) {
			EditBox nameBox = new EditBox(font, detailLeft(), nameTop(), DETAIL_WIDTH, NAME_HEIGHT, Component.translatable("gui.cassonade.stable.name"));
			nameBox.setMaxLength(Stables.MAX_NAME_LENGTH);
			nameBox.setValue(name);
			if (entry.entity() != null) {
				nameBox.setHint(entry.entity().getType().getDescription());
			}
			nameBox.setResponder(value -> name = value);
			addRenderableWidget(nameBox);
			StableListPayload.Status status = entry.horse().status();
			String action = switch (status) {
				case BOARDED -> "take_out";
				case HERE -> "board";
				case AWAY -> "call";
			};
			addRenderableWidget(Button.builder(Component.translatable("gui.cassonade.stable." + action), button -> {
				saveName();
				ClientPlayNetworking.send(new StableActionPayload(list.post(), status == StableListPayload.Status.HERE
						? StableActionPayload.Action.BOARD : StableActionPayload.Action.TAKE_OUT, entry.horse().id()));
			}).bounds(detailLeft(), bottomTop(), DETAIL_WIDTH, BUTTON_HEIGHT).build());
		}
		Entity candidate = list.candidate() < 0 ? null : minecraft.level.getEntity(list.candidate());
		if (candidate != null) {
			addRenderableWidget(Button.builder(Component.translatable("gui.cassonade.stable.register", candidate.getDisplayName()), button -> {
				saveName();
				ClientPlayNetworking.send(new StableActionPayload(list.post(), StableActionPayload.Action.REGISTER, candidate.getUUID()));
			}).bounds(listLeft(), bottomTop(), LIST_WIDTH, BUTTON_HEIGHT).build());
		}
	}

	/** The index in the list of the horse under the mouse, or -1. */
	private int rowAt(double x, double y) {
		if (x < listLeft() || x >= listLeft() + rowWidth() || y < listTop()) {
			return -1;
		}
		int row = (int) ((y - listTop()) / ROW_HEIGHT);
		return row < ROWS && row + scroll < shown.size() && y - listTop() - row * ROW_HEIGHT < ROW_HEIGHT - 2 ? row + scroll : -1;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scrollTo(scroll - (int) Math.signum(scrollY));
		return true;
	}

	private void scrollTo(int row) {
		int moved = Math.clamp(row, 0, maxScroll());
		if (moved != scroll) {
			ScrollSounds.moved(scroll, moved);
			scroll = moved;
		}
	}

	/** The handle's middle follows the mouse, the list moving by whole rows. */
	private void dragTo(double y) {
		int track = grooveBottom() - listTop() - handleHeight();
		if (track > 0) {
			scrollTo((int) Math.round((y - listTop() - handleHeight() / 2.0) / track * maxScroll()));
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			dragTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging) {
			dragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	/** The horse shown on the right, as made on the client: for the tests. */
	public @Nullable AbstractHorse shownHorse() {
		Shown entry = selectedShown();
		return entry == null ? null : entry.entity();
	}

	/** Shows that row's horse on the right, the name typed for the last one kept. */
	public void pick(int row) {
		saveName();
		select(shown.get(row));
		rebuildWidgets();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isOverScrollbar(event.x(), event.y())) {
			dragging = true;
			dragTo(event.y());
			return true;
		}
		int row = event.button() == InputConstants.MOUSE_BUTTON_LEFT ? rowAt(event.x(), event.y()) : -1;
		if (row >= 0 && !shown.get(row).horse().id().equals(selected)) {
			pick(row);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		ContainerPanel.draw(graphics, panelLeft(), panelTop(), panelWidth(), panelHeight());
		Component title = Component.translatable("gui.cassonade.stable.title", shown.size(), Stables.CAPACITY);
		graphics.text(font, title, listLeft(), panelTop() + BORDER + PADDING, ContainerPanel.LABEL, false);
		extractRows(graphics, mouseX, mouseY);
		Shown entry = selectedShown();
		if (entry != null && entry.entity() != null) {
			extractDetail(graphics, entry.entity(), mouseX, mouseY);
		} else if (entry == null) {
			graphics.centeredText(font, Component.translatable("gui.cassonade.stable.empty"), detailLeft() + DETAIL_WIDTH / 2,
					listTop() + listHeight() / 2 - font.lineHeight, TEXT);
		}
		if (list.candidate() < 0 || minecraft.level.getEntity(list.candidate()) == null) {
			Component hint = Component.translatable(shown.size() >= Stables.CAPACITY ? "gui.cassonade.stable.full" : "gui.cassonade.stable.hint");
			// On as many lines as it takes within the list's width, in the middle of the button's place
			List<FormattedCharSequence> lines = font.split(hint, (int) (LIST_WIDTH / SmallText.scale()));
			int lineHeight = SmallText.lineHeight(font);
			float top = bottomTop() + (BUTTON_HEIGHT - lines.size() * lineHeight) / 2f;
			for (int i = 0; i < lines.size(); i++) {
				float lineWidth = font.width(lines.get(i)) * SmallText.scale();
				SmallText.left(graphics, font, lines.get(i), listLeft() + (LIST_WIDTH - lineWidth) / 2, top + i * lineHeight, ContainerPanel.LABEL);
			}
		}
	}

	/**
	 * Each horse a dark slot: its head on the left, its name, and where it is under it; the one picked framed in green.
	 * Empty slots show the room left, up to the rows in view.
	 */
	private void extractRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int hovered = rowAt(mouseX, mouseY);
		int width = rowWidth();
		int smallHeight = SmallText.lineHeight(font);
		for (int i = 0; i < ROWS; i++) {
			int index = i + scroll;
			int x = listLeft();
			int y = listTop() + i * ROW_HEIGHT;
			int bottom = y + ROW_HEIGHT - 2;
			Shown entry = index < shown.size() ? shown.get(index) : null;
			if (entry == null && index >= Stables.CAPACITY) {
				continue;
			}
			boolean picked = entry != null && entry.horse().id().equals(selected);
			graphics.fill(x, y, x + width, bottom, picked ? SELECT : index == hovered ? HOVER : OUTLINE);
			graphics.fill(x + 1, y + 1, x + width - 1, bottom - 1, FILL);
			if (entry == null) {
				continue;
			}
			if (entry.entity() != null) {
				portrait(graphics, entry.entity(), x + 1, y + 1, x + 1 + PORTRAIT_SIZE, bottom - 1);
			}
			// The picked one's name as it is being typed
			String typed = picked ? name.strip() : entry.horse().name();
			Component label = typed.isEmpty() && entry.entity() != null ? entry.entity().getType().getDescription() : Component.literal(typed);
			int textX = x + 1 + PORTRAIT_SIZE + TEXT_GAP;
			// The name and the status together, in the middle of the slot's height
			int textTop = y + (ROW_HEIGHT - 2 - (font.lineHeight + STATUS_GAP + smallHeight)) / 2 + 1;
			graphics.text(font, font.substrByWidth(label, x + width - TEXT_GAP - textX).getString(), textX, textTop, TEXT, false);
			StableListPayload.Status status = entry.horse().status();
			SmallText.left(graphics, font, Component.translatable("gui.cassonade.stable.status." + status.name().toLowerCase(Locale.ROOT)),
					textX, textTop + font.lineHeight + STATUS_GAP, status == StableListPayload.Status.HERE ? HERE : FAINT);
		}
		if (maxScroll() > 0) {
			boolean over = dragging || isOverScrollbar(mouseX, mouseY);
			// The helper starts its groove 2 pixels into the list, where its first card begins
			WorldSelectionScreen.extractScrollbar(graphics, scrollbarX(), listTop() - 2, grooveBottom(), SCROLLBAR_WIDTH, (double) scroll / maxScroll(),
					handleHeight(), over);
			if (over) {
				graphics.requestCursor(dragging ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
			}
		}
	}

	/**
	 * The head, three-quarters, filling the slot's left square, as on a stable's register: the game's own drawing of a mob
	 * in a window turns it barely a third of the way, and frames it whole.
	 */
	private static void portrait(GuiGraphicsExtractor graphics, AbstractHorse horse, int x0, int y0, int x1, int y1) {
		EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(horse).createRenderState(horse, 1);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180 + PORTRAIT_TURN;
			living.yRot = 0;
			living.xRot = 0;
			living.boundingBoxWidth /= living.scale;
			living.boundingBoxHeight /= living.scale;
			living.scale = 1;
		}
		// Moved so the head, ahead of the body and above it, comes to the middle of the square
		Vector3f translation = new Vector3f(PORTRAIT_SHIFT, state.boundingBoxHeight / 2 + PORTRAIT_DROP, 0);
		graphics.entity(state, PORTRAIT_SCALE, translation, new Quaternionf().rotateZ((float) Math.PI), null, x0, y0, x1, y1);
	}

	/** The horse drawn whole, turning to the mouse, then its stats under its name. */
	private void extractDetail(GuiGraphicsExtractor graphics, AbstractHorse horse, int mouseX, int mouseY) {
		int x = detailLeft();
		int y = listTop();
		ContainerPanel.sunk(graphics, x, y, DETAIL_WIDTH, VIEW_HEIGHT, ContainerPanel.BOX_FILL);
		InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x + 1, y + 1, x + DETAIL_WIDTH - 1, y + VIEW_HEIGHT - 1, 20, 0.1F, mouseX, mouseY, horse);
		List<HorseStats.Stat> stats = HorseStats.of(horse);
		int lineHeight = SmallText.lineHeight(font) + 3;
		int nameWidth = stats.stream().mapToInt(stat -> SmallText.width(font, stat.name())).max().orElse(0);
		for (int i = 0; i < stats.size(); i++) {
			HorseStats.Stat stat = stats.get(i);
			int rowY = statsTop() + i * lineHeight;
			SmallText.left(graphics, font, stat.name(), x, rowY, TEXT);
			int barX = x + nameWidth + STAT_GAP;
			HorseStats.bar(graphics, barX, rowY + (SmallText.lineHeight(font) - HorseStats.BAR_HEIGHT) / 2, stat.fill());
			SmallText.left(graphics, font, stat.value(), barX + HorseStats.BAR_WIDTH + STAT_GAP, rowY, TEXT);
		}
	}

	private void saveName() {
		Shown entry = selectedShown();
		if (entry != null && !name.strip().equals(entry.horse().name())) {
			ClientPlayNetworking.send(new StableRenamePayload(list.post(), entry.horse().id(), name));
			// Shown at once in its row, as the server will send it next
			StableListPayload.Horse horse = entry.horse();
			shown.set(shown.indexOf(entry), new Shown(new StableListPayload.Horse(horse.id(), horse.entity(), name.strip(), horse.status(), horse.horse()),
					entry.entity()));
		}
	}

	@Override
	public void onClose() {
		saveName();
		super.onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// The inventory's key closes it, as it closes the game's windows, unless a name is being typed
		if (event.key() == InputConstants.KEY_RETURN || !(getFocused() instanceof EditBox box && box.isFocused()) && minecraft.options.keyInventory.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
