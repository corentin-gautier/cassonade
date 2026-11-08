package com.cassonade.stable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StringUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * Stables, as at the inns of the old roads: at a hitching post, a player registers their tamed horses (donkeys, mules,
 * llamas and camels too), names them for free, sees their stats, boards them (out of the world, safe) and takes them
 * out at any post, or calls back one left far away, loaded or not, in any dimension.
 * <p>
 * A horse called back from an unloaded chunk is made again from what it was when its chunk was saved; the copy left in
 * that chunk is told apart by its UUID, a new one each time the horse is taken out, and removed as soon as it loads.
 */
public final class Stables {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Cassonade.id("hitching_post"));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Cassonade.id("hitching_post"));
	public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY,
			new HitchingPostBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
					.sound(SoundType.WOOD).strength(2).ignitedByLava().noOcclusion().setId(BLOCK_KEY)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
			new BlockItem(BLOCK, new Item.Properties().useBlockDescriptionPrefix().setId(ITEM_KEY)));
	/** As many as a player can keep track of, as in the stables this comes from. */
	public static final int CAPACITY = 8;
	public static final int MAX_NAME_LENGTH = 32;
	/** How far from the post a horse is "here": boarded from there, and registered. */
	public static final double NEAR = 10;
	private static final double REACH_MARGIN = 2;
	/** How far around the post a horse taken out may stand. */
	private static final int ARRIVAL_RADIUS = 3;
	/** One village animal pen or stable in this many gets a post by its fence. */
	private static final int PEN_ODDS = 2;

	/** Carried by a registered horse: whose it is, and its entry in their stable. */
	public record Tag(UUID owner, UUID id) {
		static final Codec<Tag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("owner").forGetter(Tag::owner),
				UUIDUtil.CODEC.fieldOf("id").forGetter(Tag::id)).apply(instance, Tag::new));
	}

	public static final AttachmentType<Tag> TAG = AttachmentRegistry.create(Cassonade.id("stable_horse"), builder -> builder.persistent(Tag.CODEC));
	/** Old copies of horses taken out since, found as their chunk loads: removed after the tick, not while the level adds them. */
	private static final List<Entity> stale = new ArrayList<>();

	private Stables() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.insertAfter(Items.LEAD, ITEM));
		PayloadTypeRegistry.clientboundPlay().register(StableListPayload.TYPE, StableListPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StableActionPayload.TYPE, StableActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StableRenamePayload.TYPE, StableRenamePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(StableActionPayload.TYPE, (payload, context) -> act(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(StableRenamePayload.TYPE, (payload, context) -> rename(context.player(), payload));
		UseBlockCallback.EVENT.register(Stables::tie);
		ServerEntityEvents.ENTITY_LOAD.register(Stables::loaded);
		ServerEntityEvents.ENTITY_UNLOAD.register(Stables::unloaded);
		ServerLivingEntityEvents.AFTER_DEATH.register(Stables::died);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			stale.forEach(Entity::discard);
			stale.clear();
		});
	}

	/**
	 * Sneaking at a post, the animals the player leads are tied to it, as to a fence, whatever is in hand: the game skips
	 * the block when sneaking with an item, and a lead only knows fences (LeashFenceKnotEntityMixin keeps the knot there).
	 * Without any, sneaking places blocks against it as usual.
	 */
	private static InteractionResult tie(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || player.isSpectator() || !level.getBlockState(pos).is(BLOCK)
				|| Leashable.leashableInArea(level, Vec3.atCenterOf(pos), leashable -> leashable.getLeashHolder() == player).isEmpty()) {
			return InteractionResult.PASS;
		}
		return level.isClientSide() ? InteractionResult.SUCCESS : LeadItem.bindPlayerMobs(player, level, pos);
	}

	/** The entry of the horse, if it is the registered one and out in the world: not a copy left behind, nor one released. */
	private static Optional<StableData.Entry> entry(MinecraftServer server, Entity entity) {
		Tag tag = entity.getAttached(TAG);
		if (tag == null) {
			return Optional.empty();
		}
		return StableData.of(server).get(tag.owner(), tag.id()).filter(entry -> !entry.boarded() && entry.entity().equals(entity.getUUID()));
	}

	/** A copy of a horse taken out since goes; the real one gets the name it was given at a post while its chunk slept. */
	private static void loaded(Entity entity, ServerLevel level) {
		Tag tag = entity.getAttached(TAG);
		if (tag == null || !(entity instanceof AbstractHorse horse)) {
			return;
		}
		Optional<StableData.Entry> entry = StableData.of(level.getServer()).get(tag.owner(), tag.id());
		if (entry.isEmpty()) {
			entity.removeAttached(TAG);
		} else if (entry.get().boarded() || !entry.get().entity().equals(entity.getUUID())) {
			stale.add(entity);
		} else {
			name(horse, entry.get().name());
		}
	}

	/** As its chunk is saved, so is the horse in its entry: called back from there, it is made again as it was. */
	private static void unloaded(Entity entity, ServerLevel level) {
		Entity.RemovalReason reason = entity.getRemovalReason();
		if (entity instanceof AbstractHorse horse && reason != null && !reason.shouldDestroy()) {
			entry(level.getServer(), horse).ifPresent(entry -> StableData.of(level.getServer()).put(tag(horse).owner(), seen(entry, horse)));
		}
	}

	private static void died(LivingEntity entity, DamageSource source) {
		if (entity instanceof AbstractHorse horse && entity.level() instanceof ServerLevel level) {
			entry(level.getServer(), horse).ifPresent(entry -> StableData.of(level.getServer()).remove(tag(horse).owner(), entry.id()));
		}
	}

	private static Tag tag(Entity entity) {
		return entity.getAttachedOrThrow(TAG);
	}

	/** The entry with the horse as it is now, its name included. */
	private static StableData.Entry seen(StableData.Entry entry, AbstractHorse horse) {
		String name = horse.getCustomName() == null ? "" : horse.getCustomName().getString();
		return new StableData.Entry(entry.id(), horse.getUUID(), name, false, save(horse));
	}

	private static TypedEntityData<EntityType<?>> save(AbstractHorse horse) {
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(horse.problemPath(), LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(reporter, horse.registryAccess());
			horse.saveWithoutId(output);
			return TypedEntityData.of(horse.getType(), output.buildResult());
		}
	}

	private static void name(AbstractHorse horse, String name) {
		horse.setCustomName(name.isBlank() ? null : Component.literal(name));
	}

	/** The horse out in the world, in whichever dimension, if its chunk is loaded. */
	private static @Nullable AbstractHorse find(MinecraftServer server, StableData.Entry entry) {
		if (entry.boarded()) {
			return null;
		}
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(entry.entity()) instanceof AbstractHorse horse && horse.isAlive()) {
				return horse;
			}
		}
		return null;
	}

	private static boolean isHere(@Nullable AbstractHorse horse, ServerLevel level, BlockPos post) {
		return horse != null && horse.level() == level && horse.position().closerThan(Vec3.atBottomCenterOf(post), NEAR);
	}

	private static boolean atPost(ServerPlayer player, BlockPos post) {
		return player.level().getBlockState(post).is(BLOCK) && !player.isSpectator() && player.isWithinBlockInteractionRange(post, REACH_MARGIN);
	}

	static void openList(ServerPlayer player, BlockPos post) {
		MinecraftServer server = player.level().getServer();
		StableData data = StableData.of(server);
		List<StableListPayload.Horse> horses = new ArrayList<>();
		for (StableData.Entry entry : data.of(player.getUUID())) {
			AbstractHorse horse = find(server, entry);
			if (horse != null) {
				// Seen now: the screen shows it as it is, and calling it back from an unloaded chunk later starts from here
				entry = seen(entry, horse);
				data.put(player.getUUID(), entry);
			}
			StableListPayload.Status status = entry.boarded() ? StableListPayload.Status.BOARDED
					: isHere(horse, player.level(), post) ? StableListPayload.Status.HERE : StableListPayload.Status.AWAY;
			horses.add(new StableListPayload.Horse(entry.id(), entry.entity(), entry.name(), status, entry.horse()));
		}
		AbstractHorse candidate = horses.size() < CAPACITY ? candidate(player, post) : null;
		ServerPlayNetworking.send(player, new StableListPayload(post, horses, candidate == null ? -1 : candidate.getId()));
	}

	/** The horse the player rides, or else the nearest of theirs around the post, tamed and not registered yet. */
	private static @Nullable AbstractHorse candidate(ServerPlayer player, BlockPos post) {
		if (player.getVehicle() instanceof AbstractHorse ridden && canRegister(player, ridden, post)) {
			return ridden;
		}
		return player.level().getEntitiesOfClass(AbstractHorse.class, new AABB(post).inflate(NEAR), horse -> canRegister(player, horse, post)).stream()
				.min(Comparator.comparingDouble(horse -> horse.distanceToSqr(Vec3.atCenterOf(post)))).orElse(null);
	}

	private static boolean canRegister(ServerPlayer player, AbstractHorse horse, BlockPos post) {
		EntityReference<LivingEntity> owner = horse.getOwnerReference();
		return horse.isAlive() && horse.isTamed() && owner != null && owner.getUUID().equals(player.getUUID()) && isHere(horse, player.level(), post)
				&& entry(player.level().getServer(), horse).isEmpty();
	}

	private static void act(ServerPlayer player, StableActionPayload payload) {
		if (!atPost(player, payload.post())) {
			return;
		}
		switch (payload.action()) {
			case REGISTER -> register(player, payload.post(), payload.horse());
			case BOARD -> board(player, payload.post(), payload.horse());
			case TAKE_OUT -> takeOut(player, payload.post(), payload.horse());
		}
		openList(player, payload.post());
	}

	private static void register(ServerPlayer player, BlockPos post, UUID horseUuid) {
		StableData data = StableData.of(player.level().getServer());
		if (data.of(player.getUUID()).size() >= CAPACITY || !(player.level().getEntity(horseUuid) instanceof AbstractHorse horse)
				|| !canRegister(player, horse, post)) {
			return;
		}
		Tag tag = new Tag(player.getUUID(), UUID.randomUUID());
		horse.setAttached(TAG, tag);
		data.add(player.getUUID(), seen(new StableData.Entry(tag.id(), horse.getUUID(), "", false, save(horse)), horse));
		horse.playAmbientSound();
	}

	/** Out of the world, as it is now: its rider set down, its lead dropped. */
	private static void board(ServerPlayer player, BlockPos post, UUID id) {
		StableData data = StableData.of(player.level().getServer());
		Optional<StableData.Entry> entry = data.get(player.getUUID(), id);
		AbstractHorse horse = entry.map(found -> find(player.level().getServer(), found)).orElse(null);
		if (horse == null || !isHere(horse, player.level(), post)) {
			return;
		}
		leave(horse);
		data.put(player.getUUID(), new StableData.Entry(id, horse.getUUID(), entry.get().name(), true, save(horse)));
		player.level().playSound(null, post, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1, 1);
		horse.discard();
	}

	private static void leave(AbstractHorse horse) {
		horse.ejectPassengers();
		if (horse.isLeashed()) {
			horse.dropLeash();
		}
	}

	/** At the post's foot: out of the stable, or called back from wherever it is, as it was last seen there. */
	private static void takeOut(ServerPlayer player, BlockPos post, UUID id) {
		ServerLevel level = player.level();
		StableData data = StableData.of(level.getServer());
		Optional<StableData.Entry> found = data.get(player.getUUID(), id);
		if (found.isEmpty()) {
			return;
		}
		StableData.Entry entry = found.get();
		AbstractHorse loaded = find(level.getServer(), entry);
		if (isHere(loaded, level, post)) {
			return;
		}
		EntityType<?> type = entry.horse().type();
		Optional<Vec3> spot = arrival(level, post, type, player);
		if (spot.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("gui.cassonade.stable.blocked"));
			return;
		}
		TypedEntityData<EntityType<?>> saved = entry.horse();
		if (loaded != null) {
			leave(loaded);
			saved = save(loaded);
		}
		CompoundTag tag = saved.copyTagWithoutId();
		tag.remove("Passengers");
		tag.remove("leash");
		Entity made = EntityType.loadEntityRecursive(type, tag, level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
		if (!(made instanceof AbstractHorse horse)) {
			return;
		}
		// A new UUID, so the copy left in an unloaded chunk is known for one when it loads
		horse.setUUID(UUID.randomUUID());
		Vec3 at = spot.get();
		horse.snapTo(at.x, at.y, at.z, (float) Math.toDegrees(Math.atan2(post.getX() + 0.5 - at.x, at.z - post.getZ() - 0.5)), 0);
		horse.setDeltaMovement(Vec3.ZERO);
		horse.resetFallDistance();
		horse.setAttached(TAG, new Tag(player.getUUID(), id));
		name(horse, entry.name());
		data.put(player.getUUID(), new StableData.Entry(id, horse.getUUID(), entry.name(), false, saved));
		if (loaded != null) {
			loaded.discard();
		}
		level.addFreshEntity(horse);
		level.playSound(null, horse.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.NEUTRAL, 1, 1);
	}

	/**
	 * The nearest spot around the post where the horse stands on ground, with room for it, out of any fluid, and not on the
	 * player: in their way, it would take the next click meant for the post.
	 */
	private static Optional<Vec3> arrival(ServerLevel level, BlockPos post, EntityType<?> type, ServerPlayer player) {
		AABB around = player.getBoundingBox().inflate(1);
		return BlockPos.betweenClosedStream(post.offset(-ARRIVAL_RADIUS, -1, -ARRIVAL_RADIUS), post.offset(ARRIVAL_RADIUS, 1, ARRIVAL_RADIUS))
				.map(BlockPos::immutable)
				.filter(pos -> !pos.equals(post) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
				.sorted(Comparator.comparingDouble(pos -> pos.distSqr(post)))
				.map(Vec3::atBottomCenterOf)
				.filter(at -> {
					AABB box = type.getSpawnAABB(at.x, at.y, at.z);
					return level.noCollision(box) && !level.containsAnyLiquid(box) && !box.intersects(around);
				})
				.findFirst();
	}

	/** Blank gives the horse back its kind's name. */
	private static void rename(ServerPlayer player, StableRenamePayload payload) {
		StableData data = StableData.of(player.level().getServer());
		Optional<StableData.Entry> entry = data.get(player.getUUID(), payload.horse());
		if (!atPost(player, payload.post()) || entry.isEmpty()) {
			return;
		}
		String name = StringUtil.filterText(payload.name()).strip();
		name = name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH));
		StableData.Entry old = entry.get();
		data.put(player.getUUID(), new StableData.Entry(old.id(), old.entity(), name, old.boarded(), old.horse()));
		AbstractHorse horse = find(player.level().getServer(), old);
		if (horse != null) {
			name(horse, name);
		}
	}

	/**
	 * A post by the fence of a village's animal pen or stable, one in PEN_ODDS, chosen from its place alone: laid by the
	 * chunk holding the piece's middle, so two chunks never both lay one. On the ground, beside a fence, its rail along it.
	 */
	public static void inAnimalPen(WorldGenLevel level, StructurePiece piece, BoundingBox chunkBB) {
		if (!(piece instanceof PoolElementStructurePiece pen)) {
			return;
		}
		String element = pen.getElement().toString();
		if (!element.contains("_animal_pen_") && !element.contains("_stable_")) {
			return;
		}
		BoundingBox box = piece.getBoundingBox();
		int middleX = (box.minX() + box.maxX()) / 2;
		int middleZ = (box.minZ() + box.maxZ()) / 2;
		if (middleX < chunkBB.minX() || middleX > chunkBB.maxX() || middleZ < chunkBB.minZ() || middleZ > chunkBB.maxZ()
				|| Math.floorMod(Long.hashCode(BlockPos.asLong(box.minX(), box.minY(), box.minZ()) * 0x9E3779B97F4A7C15L), PEN_ODDS) != 0) {
			return;
		}
		for (BlockPos pos : BlockPos.betweenClosed(Math.max(box.minX(), chunkBB.minX()), box.minY(), Math.max(box.minZ(), chunkBB.minZ()),
				Math.min(box.maxX(), chunkBB.maxX()), Math.min(box.maxY(), box.minY() + 3), Math.min(box.maxZ(), chunkBB.maxZ()))) {
			Direction fence = byFence(level, pos);
			if (fence != null) {
				level.setBlock(pos, BLOCK.defaultBlockState().setValue(HitchingPostBlock.FACING, fence), Block.UPDATE_CLIENTS);
				return;
			}
		}
	}

	/** The side of a fence the spot is on, if it is free ground beside one: air there and above, solid ground under it. */
	private static @Nullable Direction byFence(WorldGenLevel level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir() || below.is(BlockTags.FENCES)
				|| !below.isFaceSturdy(level, pos.below(), Direction.UP)) {
			return null;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(side)).is(BlockTags.FENCES)) {
				return side;
			}
		}
		return null;
	}
}
