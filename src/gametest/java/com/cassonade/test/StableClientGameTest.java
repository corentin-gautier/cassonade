package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.phys.AABB;

import com.cassonade.client.stable.StableScreen;
import com.cassonade.stable.StableActionPayload;
import com.cassonade.stable.StableData;

/**
 * The hitching post: a tamed horse registered, named for free, boarded out of the world and taken out again; another
 * left far away called back from its unloaded chunk, the copy left there gone as it loads; a horse that dies leaves the
 * stable.
 */
public class StableClientGameTest implements FabricClientGameTest {
	private static final BlockPos POST = new BlockPos(4, 101, 2);
	private static final int FAR = 400;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("setblock 4 101 2 cassonade:hitching_post[facing=west]");
			UUID first = world.getServer().computeOnServer(server -> tamed(server, 9.5, 6.5).getUUID());

			// Its horse standing by: offered to register
			use(context, world);
			context.waitForScreen(StableScreen.class);
			press(context, "gui.cassonade.stable.register");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> check(StableData.of(server).of(player(server).getUUID()).size() == 1, "the horse registered"));
			context.runOnClient(client -> check(button(client.gui.screen().children(), "gui.cassonade.stable.board") != null, "the registered horse can be boarded"));

			// Named for free from the screen
			context.runOnClient(client -> client.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
					.findFirst().orElseThrow().setValue(" Biscuit "));
			ScreenInput.pressOnScreen(context, InputConstants.KEY_RETURN);
			context.waitTicks(5);
			world.getServer().runOnServer(server -> check(horse(server, first).getCustomName().getString().equals("Biscuit"), "the horse named"));

			// The inventory's key and mouse button 4 close it, as the game's windows
			use(context, world);
			context.waitForScreen(StableScreen.class);
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitTicks(3);
			context.runOnClient(client -> check(!(client.gui.screen() instanceof StableScreen), "closed by the inventory's key"));
			use(context, world);
			context.waitForScreen(StableScreen.class);
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_4);
			context.waitTicks(3);
			context.runOnClient(client -> check(!(client.gui.screen() instanceof StableScreen), "closed by mouse button 4"));

			// Walking away while it is open, it can no longer be boarded, only called back; coming back, it can again
			use(context, world);
			context.waitForScreen(StableScreen.class);
			world.getServer().runCommand("fill 38 100 0 42 100 4 stone");
			world.getServer().runCommand("fill 38 101 0 42 103 4 air");
			world.getServer().runOnServer(server -> horse(server, first).teleportTo(40.5, 101, 2.5));
			context.waitTicks(10);
			context.runOnClient(client -> check(button(client.gui.screen().children(), "gui.cassonade.stable.board") == null
					&& button(client.gui.screen().children(), "gui.cassonade.stable.call") != null, "a horse gone away called back, not boarded"));
			world.getServer().runOnServer(server -> horse(server, first).teleportTo(9.5, 101, 6.5));
			context.waitTicks(10);
			context.runOnClient(client -> check(button(client.gui.screen().children(), "gui.cassonade.stable.board") != null, "a horse back by the post boarded"));
			context.setScreen(() -> null);

			// Sneaking, the horse led is tied to the post, and stays tied
			// With something in hand too, as leads: the game skips the block when sneaking with an item
			world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:lead 3");
			// Beside the post, out of the way of the click: led, it is pulled towards the player
			world.getServer().runOnServer(server -> {
				horse(server, first).setLeashedTo(player(server), true);
				horse(server, first).teleportTo(5.5, 101, 5.5);
			});
			context.getInput().holdKey(options -> options.keyShift);
			use(context, world);
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(40);
			world.getServer().runOnServer(server -> check(horse(server, first).getLeashHolder() instanceof LeashFenceKnotEntity, "the horse tied to the post"));
			context.runOnClient(client -> check(client.gui.screen() == null, "no screen when tying"));
			// Up close, the knot wrapped around the post
			world.getServer().runCommand("tp @a 3.1 101 4.4 -138.8 21");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-stable-tied");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");

			// Boarded: out of the world, kept as it was
			use(context, world);
			context.waitForScreen(StableScreen.class);
			float health = world.getServer().computeOnServer(server -> horse(server, first).getMaxHealth());
			context.runOnClient(client -> check(((StableScreen) client.gui.screen()).shownHorse().getMaxHealth() == health, "the horse's own stats shown"));
			Screenshots.take(context, "cassonade-stable");
			press(context, "gui.cassonade.stable.board");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				check(horse(server, first) == null, "the boarded horse out of the world");
				check(StableData.of(server).of(player(server).getUUID()).getFirst().boarded(), "the horse boarded");
			});

			// Taken out at the post's foot, its name kept
			press(context, "gui.cassonade.stable.take_out");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				AbstractHorse out = near(server.overworld(), POST, 4).getFirst();
				check(out.getCustomName() != null && out.getCustomName().getString().equals("Biscuit"), "the horse taken out with its name");
				check(!StableData.of(server).of(player(server).getUUID()).getFirst().boarded(), "the horse out");
			});
			context.setScreen(() -> null);

			// Another, registered here, then left far away in a chunk that unloads
			world.getServer().runOnServer(server -> tamed(server, 7.5, 8.5));
			use(context, world);
			context.waitForScreen(StableScreen.class);
			press(context, "gui.cassonade.stable.register");
			context.waitTicks(5);
			context.setScreen(() -> null);
			PlannedWorld.load(context, world, FAR - 8, FAR - 8, FAR + 8, FAR + 8);
			world.getServer().runCommand("fill " + (FAR - 4) + " 100 " + (FAR - 4) + " " + (FAR + 4) + " 100 " + (FAR + 4) + " stone");
			world.getServer().runCommand("fill " + (FAR - 4) + " 101 " + (FAR - 4) + " " + (FAR + 4) + " 104 " + (FAR + 4) + " air");
			world.getServer().runOnServer(server -> {
				AbstractHorse second = near(server.overworld(), new BlockPos(7, 101, 8), 1).getFirst();
				second.teleportTo(FAR + 0.5, 101, FAR + 0.5);
			});
			context.waitTicks(5);
			world.getServer().runCommand("forceload remove " + (FAR - 8) + " " + (FAR - 8) + " " + (FAR + 8) + " " + (FAR + 8));
			for (int i = 0; i < 20 * 30 && world.getServer().computeOnServer(server -> server.overworld().isLoaded(new BlockPos(FAR, 101, FAR))); i++) {
				context.waitTick();
			}
			check(!world.getServer().computeOnServer(server -> server.overworld().isLoaded(new BlockPos(FAR, 101, FAR))), "the far chunk unloaded");
			context.waitTicks(20);

			// Called back from there: here at once, and the copy left behind gone as its chunk loads
			use(context, world);
			context.waitForScreen(StableScreen.class);
			context.runOnClient(client -> {
				StableScreen screen = (StableScreen) client.gui.screen();
				screen.pick(1);
			});
			press(context, "gui.cassonade.stable.call");
			context.waitTicks(5);
			context.setScreen(() -> null);
			world.getServer().runOnServer(server -> check(near(server.overworld(), POST, 4).size() == 2, "the far horse called back to the post"));
			PlannedWorld.load(context, world, FAR - 8, FAR - 8, FAR + 8, FAR + 8);
			context.waitTicks(20);
			world.getServer().runOnServer(server -> check(near(server.overworld(), new BlockPos(FAR, 101, FAR), 8).isEmpty(), "the copy left far away gone"));
			world.getServer().runCommand("forceload remove " + (FAR - 8) + " " + (FAR - 8) + " " + (FAR + 8) + " " + (FAR + 8));

			// A horse that dies leaves the stable
			world.getServer().runOnServer(server -> near(server.overworld(), POST, 4).getFirst().kill(server.overworld()));
			context.waitTicks(5);
			world.getServer().runOnServer(server -> check(StableData.of(server).of(player(server).getUUID()).size() == 1, "a dead horse struck off"));

			// More horses than the rows in view: the list scrolls
			List<UUID> more = world.getServer().computeOnServer(server -> IntStream.range(0, 6)
					.mapToObj(i -> tamed(server, 6.5 + i, 7.5).getUUID()).toList());
			for (UUID horse : more) {
				context.runOnClient(client -> ClientPlayNetworking.send(new StableActionPayload(POST, StableActionPayload.Action.REGISTER, horse)));
			}
			context.waitTicks(5);
			world.getServer().runOnServer(server -> check(StableData.of(server).of(player(server).getUUID()).size() == 7, "seven horses registered"));
			use(context, world);
			context.waitForScreen(StableScreen.class);
			context.runOnClient(client -> check(((StableScreen) client.gui.screen()).maxScroll() == 2, "the list scrolls past its five rows"));
			context.runOnClient(client -> client.gui.screen().mouseScrolled(client.gui.screen().width / 2.0, client.gui.screen().height / 2.0, 0, -1));
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-stable-scrolled");
			context.setScreen(() -> null);
		}
	}

	private static AbstractHorse tamed(MinecraftServer server, double x, double z) {
		ServerLevel level = server.overworld();
		Horse horse = EntityTypes.HORSE.create(level, EntitySpawnReason.COMMAND);
		horse.snapTo(x, 101, z, 0, 0);
		// Its stats drawn as for a horse born in the world
		horse.finalizeSpawn(level, level.getCurrentDifficultyAt(horse.blockPosition()), EntitySpawnReason.COMMAND, null);
		horse.tameWithName(player(server));
		level.addFreshEntity(horse);
		return horse;
	}

	private static AbstractHorse horse(MinecraftServer server, UUID uuid) {
		return server.overworld().getEntity(uuid) instanceof AbstractHorse horse ? horse : null;
	}

	private static List<AbstractHorse> near(ServerLevel level, BlockPos pos, double radius) {
		return level.getEntitiesOfClass(AbstractHorse.class, new AABB(pos).inflate(radius), AbstractHorse::isAlive);
	}

	/** Standing west of the post, looking at it. */
	private static void use(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("tp @a 2.5 101 2.5 -90 25");
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
	}

	private static Button button(List<? extends Object> children, String key) {
		String label = Component.translatable(key).getString();
		return children.stream().filter(Button.class::isInstance).map(Button.class::cast)
				.filter(button -> button.getMessage().getString().startsWith(label.replace("%s", "").strip())).findFirst().orElse(null);
	}

	private static void press(ClientGameTestContext context, String key) {
		context.runOnClient(client -> {
			Button button = button(client.gui.screen().children(), key);
			check(button != null, "a button " + key);
			button.onPress(null);
		});
	}
}
