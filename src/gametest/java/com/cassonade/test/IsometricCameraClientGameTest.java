package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;

import com.cassonade.client.IsometricCamera;

public class IsometricCameraClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				for (CameraType expected : new CameraType[] {CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT}) {
					client.options.setCameraType(client.options.getCameraType().cycle());
					check(client.options.getCameraType() == expected && !IsometricCamera.isActive(), "vanilla's views first");
				}
				client.options.setCameraType(client.options.getCameraType().cycle());
				check(IsometricCamera.isActive(), "the isometric view after them");
			});
			context.waitTicks(5);
			context.runOnClient(client -> {
				check(Math.abs(client.gameRenderer.mainCamera().xRot() - IsometricCamera.PITCH) < 0.01F, "the camera looking down");
				check(client.gameRenderer.mainCamera().yRot() == IsometricCamera.yaw(), "the camera on its diagonal");
			});
			Screenshots.take(context, "cassonade-isometric-camera");
			context.runOnClient(client -> {
				check(!client.mouseHandler.isMouseGrabbed(), "the mouse free");
				client.options.keyLeft.setDown(true);
			});
			context.waitTicks(3);
			context.runOnClient(client -> {
				client.options.keyLeft.setDown(false);
				float turned = net.minecraft.util.Mth.wrapDegrees(client.player.getYRot() - (IsometricCamera.yaw() - 90));
				check(Math.abs(turned) < 0.01F, "the left key turning the player to the screen's left, off by " + turned);
				double distance = client.gameRenderer.mainCamera().position().distanceTo(client.player.getEyePosition());
				check(Math.abs(distance - IsometricCamera.DISTANCE) < 1, "the camera at its distance, found " + distance);
			});
			// The cursor at the middle of the screen aims at the ground by the player's feet, in reach; at the top, far beyond it
			context.runOnClient(client -> {
				IsometricCamera.aim(client, 0.5, 0.55);
				check(client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK, "the block under the cursor, found " + client.hitResult.getType());
				IsometricCamera.aim(client, 0.5, 0.2);
				check(client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS, "nothing out of reach, found " + client.hitResult.getType());
			});
			// Turning the view takes the camera with it
			context.runOnClient(client -> IsometricCamera.turn(100));
			context.waitTicks(2);
			context.runOnClient(client -> check(client.gameRenderer.mainCamera().yRot() == IsometricCamera.yaw(), "the camera following its turn"));
			// On a horse and in a boat, the keys point along the screen just the same
			world.getServer().runCommand("execute as @p at @s run summon horse ~ ~ ~ {Tame:1b,equipment:{saddle:{id:\"minecraft:saddle\"}}}");
			world.getServer().runCommand("ride @p mount @e[type=horse,limit=1]");
			steer(context, "the horse");
			world.getServer().runCommand("ride @p dismount");
			world.getServer().runCommand("kill @e[type=horse]");
			world.getServer().runCommand("execute as @p at @s run summon oak_boat ~ ~ ~");
			world.getServer().runCommand("ride @p mount @e[type=oak_boat,limit=1]");
			steer(context, "the boat");
			world.getServer().runCommand("ride @p dismount");
			world.getServer().runCommand("kill @e[type=oak_boat]");
			context.waitTicks(5);
			// A wall right behind the player doesn't bring the camera closer
			world.getServer().runCommand("execute as @p at @s run fill ~-3 ~ ~-3 ~3 ~5 ~3 minecraft:stone hollow");
			context.waitTicks(5);
			context.runOnClient(client -> {
				double distance = client.gameRenderer.mainCamera().position().distanceTo(client.player.getEyePosition());
				check(Math.abs(distance - IsometricCamera.DISTANCE) < 1, "the camera staying at its distance, found " + distance);
			});
			context.runOnClient(client -> {
				client.options.setCameraType(client.options.getCameraType().cycle());
				check(client.options.getCameraType() == CameraType.FIRST_PERSON && !IsometricCamera.isActive(), "back to first person");
			});
		}
	}

	private static void steer(ClientGameTestContext context, String what) {
		context.waitTicks(10);
		context.runOnClient(client -> client.options.keyRight.setDown(true));
		context.waitTicks(100);
		context.runOnClient(client -> {
			client.options.keyRight.setDown(false);
			float off = net.minecraft.util.Mth.wrapDegrees(client.player.getVehicle().getYRot() - (IsometricCamera.yaw() + 90));
			check(Math.abs(off) < 12, what + " turned to the screen's right, off by " + off);
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
