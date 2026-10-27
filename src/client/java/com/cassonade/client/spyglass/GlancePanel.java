package com.cassonade.client.spyglass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.map.FramedPanel;
import com.cassonade.client.map.SmallText;
import com.cassonade.item.CassonadeItems;
import com.cassonade.spyglass.GlancePayload;
import com.cassonade.spyglass.GlanceRequestPayload;

/**
 * What the player sees through the naturalist's spyglass, in a small panel under the crosshair, up to 128 blocks
 * away: a horse's speed, jump and health, and a llama's strength, each as a bar from the worst a horse can be born
 * with to the best, red to green; a villager's
 * level and its sold-out trades; whether an animal can breed, or when it grows up; a crop's growth; a beehive's
 * honey; a note block's note, which also shows without the spyglass, at arm's length. Only the server knows
 * a mob's age and a villager's trades: they are asked for every second (GlanceRequestPayload), the times
 * counted down in between. Nothing shows for anything else.
 */
public final class GlancePanel {
	private static final int ASK_INTERVAL = 20;
	private static final int LOWEST_NOTE_MIDI = 54;
	private static final String[] NOTE_NAMES = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B"};
	/** How much a mob is widened to aim at it. */
	private static final double AIM_MARGIN = 0.3;
	/** Blocks per second for one point of movement speed, on flat ground. */
	private static final double SPEED_TO_BLOCKS = 42.16;
	/** The worst and the best a horse can be born with: its speed attribute, its jump strength, its hearts; a llama's strength. */
	private static final double WORST_SPEED = 0.1125 * SPEED_TO_BLOCKS;
	private static final double BEST_SPEED = 0.3375 * SPEED_TO_BLOCKS;
	private static final double WORST_JUMP = jumpHeight(0.4);
	private static final double BEST_JUMP = jumpHeight(1.0);
	private static final double WORST_HEALTH = 7.5;
	private static final double BEST_HEALTH = 15;
	private static final int WORST_STRENGTH = 1;
	private static final int BEST_STRENGTH = 5;
	private static final int PADDING = 3;
	private static final int LINE_GAP = 1;
	private static final int BELOW_CROSSHAIR = 14;
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	/** The quest tracker's hint. */
	private static final int TEXT_COLOR = 0xFFD2BBB5;
	private static final int BAR_WIDTH = 40;
	private static final int BAR_HEIGHT = 5;
	/** Between a stat's name, its bar and its value. */
	private static final int COLUMN_GAP = 4;
	/** The bar's frame and empty part, as the slots' (tools/gui_theme.py OUTLINE, FILL). */
	private static final int BAR_OUTLINE = 0xFF160608;
	private static final int BAR_EMPTY = 0xFF2E1D1C;
	/** From the worst horse to the best: the theme's MOB_HEALTH, GOLD and SELECT. */
	private static final int WORST_COLOR = 0xFFCD3336;
	private static final int MIDDLE_COLOR = 0xFFDEA62B;
	private static final int BEST_COLOR = 0xFF7CA72E;

	/** A line of the panel: some text, centered, or a stat as a bar between its name and its value. */
	private sealed interface Row {
		String text();
	}

	private record Line(Component component) implements Row {
		@Override
		public String text() {
			return component.getString();
		}
	}

	private record Stat(Component name, double fill, Component value) implements Row {
		@Override
		public String text() {
			return name.getString() + " " + value.getString();
		}
	}

	private static List<Row> lines = List.of();
	private static int ticks;
	private static int askedFor = -1;
	private static int askedAt;
	private static @Nullable GlancePayload answer;
	private static int answeredAt;

	private GlancePanel() {
	}

	public static void receive(GlancePayload payload) {
		answer = payload;
		answeredAt = ticks;
	}

	public static void tick(Minecraft minecraft) {
		ticks++;
		lines = List.of();
		if (minecraft.player == null || minecraft.level == null) {
			askedFor = -1;
			answer = null;
			return;
		}
		if (!isLookingThrough(minecraft)) {
			askedFor = -1;
			answer = null;
			// A note block's note needs no spyglass: the game should show it, it is read at arm's length
			if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
				List<Row> read = new ArrayList<>();
				readNote(minecraft.level.getBlockState(hit.getBlockPos()), read);
				lines = read;
			}
			return;
		}
		HitResult looked = farPick(minecraft, GlanceRequestPayload.RANGE);
		List<Row> read = new ArrayList<>();
		if (looked instanceof EntityHitResult hit) {
			read(minecraft, hit.getEntity(), read);
		} else if (looked instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			read(minecraft.level.getBlockState(hit.getBlockPos()), read);
		}
		lines = read;
	}

	/** Looking through the naturalist's spyglass, in first person, as the zoom does. */
	private static boolean isLookingThrough(Minecraft minecraft) {
		return minecraft.options.getCameraType().isFirstPerson() && minecraft.player.isScoping()
				&& minecraft.player.getUseItem().is(CassonadeItems.NATURALIST_SPYGLASS);
	}

	/** What the camera looks at up to the range, mob or block, the plants with nothing to bump into seen through. */
	public static HitResult farPick(Minecraft minecraft, double range) {
		Entity camera = minecraft.getCameraEntity();
		Vec3 from = camera.getEyePosition();
		Vec3 direction = camera.getViewVector(1);
		HitResult block = sightLine(camera, from, from.add(direction.scale(range)), direction);
		double reach = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(from);
		Vec3 along = direction.scale(reach);
		EntityHitResult entity = null;
		double nearest = reach * reach;
		// Each mob a little larger than it is: far away, through the zoom, it is only a few pixels to aim at
		for (Entity candidate : camera.level().getEntities(camera, camera.getBoundingBox().expandTowards(along).inflate(1 + AIM_MARGIN),
				EntitySelector.CAN_BE_PICKED)) {
			Optional<Vec3> hit = candidate.getBoundingBox().inflate(AIM_MARGIN).clip(from, from.add(along));
			if (hit.isPresent() && hit.get().distanceToSqr(from) < nearest) {
				nearest = hit.get().distanceToSqr(from);
				entity = new EntityHitResult(candidate, hit.get());
			}
		}
		return entity != null ? entity : block;
	}

	/**
	 * The first block along the look that the panel reads or that stops it: grass, ferns, flowers and the other
	 * plants with nothing to bump into are looked through, so they hide neither a mob nor a crop behind them.
	 */
	private static BlockHitResult sightLine(Entity camera, Vec3 from, Vec3 to, Vec3 direction) {
		Level level = camera.level();
		Vec3 start = from;
		while (true) {
			BlockHitResult hit = level.clip(new ClipContext(start, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, camera));
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (hit.getType() == HitResult.Type.MISS || isReadable(state) || !state.getCollisionShape(level, pos).isEmpty()) {
				return hit;
			}
			// On from where the look leaves the plant's block, the way back from the end entering it there
			Vec3 out = new AABB(pos).clip(to, start).orElse(to);
			start = out.add(direction.scale(0.001));
			if (start.distanceToSqr(from) >= to.distanceToSqr(from)) {
				return BlockHitResult.miss(to, hit.getDirection(), BlockPos.containing(to));
			}
		}
	}

	private static boolean isReadable(BlockState state) {
		return state.getBlock() instanceof BeehiveBlock || isCrop(state);
	}

	private static boolean isCrop(BlockState state) {
		return state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock || state.getBlock() instanceof NetherWartBlock
				|| state.getBlock() instanceof CocoaBlock || state.getBlock() instanceof SweetBerryBushBlock || state.getBlock() instanceof PitcherCropBlock;
	}

	private static void read(Minecraft minecraft, Entity entity, List<Row> read) {
		if (entity instanceof AbstractHorse horse) {
			double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * SPEED_TO_BLOCKS;
			read.add(stat("speed", speed, WORST_SPEED, BEST_SPEED, Component.translatable("gui.cassonade.glance.speed_value", decimal(speed))));
			if (horse.getAttribute(Attributes.JUMP_STRENGTH) != null) {
				double jump = jumpHeight(horse.getAttributeValue(Attributes.JUMP_STRENGTH));
				read.add(stat("jump", jump, WORST_JUMP, BEST_JUMP, Component.translatable("gui.cassonade.glance.jump_value", decimal(jump))));
			}
			double hearts = horse.getMaxHealth() / 2;
			read.add(stat("health", hearts, WORST_HEALTH, BEST_HEALTH, Component.translatable("gui.cassonade.glance.health_value", decimal(hearts))));
			if (horse instanceof Llama llama) {
				read.add(stat("strength", llama.getStrength(), WORST_STRENGTH, BEST_STRENGTH, Component.literal(String.valueOf(llama.getStrength()))));
			}
		}
		boolean trades = false;
		if (entity instanceof Villager villager) {
			VillagerData data = villager.getVillagerData();
			trades = !data.profession().is(VillagerProfession.NONE) && !data.profession().is(VillagerProfession.NITWIT);
			if (trades) {
				read.add(new Line(Component.translatable("merchant.level." + data.level())));
			}
		}
		if (entity instanceof AgeableMob mob) {
			ask(minecraft, mob);
			if (answer != null && answer.entity() == mob.getId()) {
				// The age moves one step towards 0 every tick, since the server's answer
				int elapsed = ticks - answeredAt;
				int age = answer.age() < 0 ? Math.min(0, answer.age() + elapsed) : Math.max(0, answer.age() - elapsed);
				if (age < 0) {
					read.add(new Line(Component.translatable("gui.cassonade.glance.grows_up", time(-age))));
				} else if (mob instanceof Animal) {
					read.add(new Line(answer.inLove() ? Component.translatable("gui.cassonade.glance.in_love")
							: age > 0 ? Component.translatable("gui.cassonade.glance.breed_in", time(age))
							: Component.translatable("gui.cassonade.glance.breed_ready")));
				}
				if (trades) {
					read.add(new Line(answer.trades() < 0 ? Component.translatable("gui.cassonade.glance.trades_unknown")
							: answer.soldOut() == 0 ? Component.translatable("gui.cassonade.glance.trades_stocked")
							: Component.translatable("gui.cassonade.glance.trades_sold_out", answer.soldOut(), answer.trades())));
				}
			}
		}
		if (!read.isEmpty()) {
			read.addFirst(new Line(entity.getDisplayName()));
		}
	}

	/** Asks the server about the mob looked at, again every second while it is still looked at. */
	private static void ask(Minecraft minecraft, AgeableMob mob) {
		if ((mob.getId() != askedFor || ticks - askedAt >= ASK_INTERVAL) && ClientPlayNetworking.canSend(GlanceRequestPayload.TYPE)) {
			ClientPlayNetworking.send(new GlanceRequestPayload(mob.getId()));
			askedFor = mob.getId();
			askedAt = ticks;
		}
	}

	private static void read(BlockState state, List<Row> read) {
		if (state.getBlock() instanceof NoteBlock) {
			readNote(state, read);
			return;
		}
		if (state.getBlock() instanceof BeehiveBlock) {
			int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
			read.add(new Line(honey >= BeehiveBlock.MAX_HONEY_LEVELS ? Component.translatable("gui.cassonade.glance.honey_full")
					: Component.translatable("gui.cassonade.glance.honey", honey, BeehiveBlock.MAX_HONEY_LEVELS)));
		} else if (isCrop(state)) {
			// Each of these grows through an "age" property, from 0 to its last value
			for (var property : state.getProperties()) {
				if (property instanceof IntegerProperty age && age.getName().equals("age")) {
					int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(1);
					int value = state.getValue(age);
					read.add(new Line(value >= max ? Component.translatable("gui.cassonade.glance.ripe")
							: Component.translatable("gui.cassonade.glance.growth", value * 100 / max)));
				}
			}
		}
		if (!read.isEmpty()) {
			read.addFirst(new Line(state.getBlock().getName()));
		}
	}

	/** The note a note block plays, as a musician names it (F♯3 to F♯5), and how many clicks it is tuned up. */
	private static void readNote(BlockState state, List<Row> read) {
		if (!(state.getBlock() instanceof NoteBlock)) {
			return;
		}
		int note = state.getValue(NoteBlock.NOTE);
		// The lowest note is F♯3, MIDI 54; each click a semitone up
		int midi = LOWEST_NOTE_MIDI + note;
		String name = NOTE_NAMES[midi % 12] + (midi / 12 - 1);
		read.add(new Line(state.getBlock().getName()));
		read.add(new Line(Component.translatable("gui.cassonade.glance.note", name, note, NoteBlock.NOTE.getPossibleValues().size() - 1)));
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (lines.isEmpty() || minecraft.gui.screen() != null) {
			return;
		}
		Font font = minecraft.font;
		int lineHeight = SmallText.lineHeight(font);
		int inner = FramedPanel.BORDER + PADDING;
		// The stats in three columns, their names, bars and values each lined up
		List<Stat> stats = lines.stream().filter(Stat.class::isInstance).map(Stat.class::cast).toList();
		int nameWidth = stats.stream().mapToInt(stat -> SmallText.width(font, stat.name())).max().orElse(0);
		int valueWidth = stats.stream().mapToInt(stat -> SmallText.width(font, stat.value())).max().orElse(0);
		int statsWidth = stats.isEmpty() ? 0 : nameWidth + COLUMN_GAP + BAR_WIDTH + COLUMN_GAP + valueWidth;
		int textWidth = lines.stream().filter(Line.class::isInstance).mapToInt(line -> SmallText.width(font, ((Line) line).component())).max().orElse(0);
		int width = Math.max(statsWidth, textWidth) + 2 * inner;
		int height = lines.size() * (lineHeight + LINE_GAP) - LINE_GAP + 2 * inner;
		int center = graphics.guiWidth() / 2;
		int x = center - width / 2;
		int y = graphics.guiHeight() / 2 + BELOW_CROSSHAIR;
		FramedPanel.draw(graphics, x, y, width, height);
		int statsLeft = center - statsWidth / 2;
		for (int i = 0; i < lines.size(); i++) {
			int rowY = y + inner + i * (lineHeight + LINE_GAP);
			switch (lines.get(i)) {
				case Line line -> SmallText.centered(graphics, font, line.component(), center, rowY, i == 0 ? TITLE_COLOR : TEXT_COLOR);
				case Stat stat -> {
					SmallText.left(graphics, font, stat.name(), statsLeft, rowY, TEXT_COLOR);
					int barX = statsLeft + nameWidth + COLUMN_GAP;
					bar(graphics, barX, rowY + (lineHeight - BAR_HEIGHT) / 2, stat.fill());
					SmallText.left(graphics, font, stat.value(), barX + BAR_WIDTH + COLUMN_GAP, rowY, TEXT_COLOR);
				}
			}
		}
	}

	/** A stat's bar, filled as far as it goes from the worst to the best, red through gold to green. */
	private static void bar(GuiGraphicsExtractor graphics, int x, int y, double fill) {
		graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BAR_OUTLINE);
		graphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, BAR_EMPTY);
		float amount = (float) Mth.clamp(fill, 0, 1);
		int color = amount < 0.5F ? ARGB.srgbLerp(amount * 2, WORST_COLOR, MIDDLE_COLOR) : ARGB.srgbLerp(amount * 2 - 1, MIDDLE_COLOR, BEST_COLOR);
		int filled = Math.max(1, Mth.floor(amount * (BAR_WIDTH - 2)));
		graphics.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, color);
	}

	private static Stat stat(String name, double value, double worst, double best, Component shown) {
		return new Stat(Component.translatable("gui.cassonade.glance." + name), (value - worst) / (best - worst), shown);
	}

	/** The lines shown, as their text: for the tests. */
	public static List<String> shown() {
		return lines.stream().map(Row::text).toList();
	}

	/** How high a horse jumps, in blocks, for its jump strength (fitted to the game's physics). */
	private static double jumpHeight(double strength) {
		return -0.1817584952 * strength * strength * strength + 3.689713992 * strength * strength + 2.128599134 * strength - 0.343930367;
	}

	private static String decimal(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	/** Minutes and seconds. */
	private static String time(int ticks) {
		int seconds = Mth.ceil(ticks / 20f);
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}
}
