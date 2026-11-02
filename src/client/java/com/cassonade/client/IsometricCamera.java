package com.cassonade.client;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** The fourth view of F5, after the two third-person ones: on a diagonal and at a fixed distance, looking down. The mouse is free as in a menu and aims with the cursor, and the keys walk along the screen. Rests on the third-person back view, so the game treats it as such. */
public final class IsometricCamera {
	public static final float PITCH = 35.264F;
	public static final float DISTANCE = 14.0F;
	private static boolean active;
	private static float yaw;
	private static boolean mouseFreed;
	private static final double RAY = 128;
	private static final float TURN_PER_PIXEL = 0.3F;
	private static double lastMouseX;
	private static final float ROW_WITHIN = 60.0F;
	private static final float TURN_WITHIN = 4.0F;
	private static @Nullable Vec3 target;

	private IsometricCamera() {
	}

	/** What F5 leads to from {@code current}, or null when it isn't this view's business. */
	public static CameraType cycle(CameraType current) {
		if (current == CameraType.THIRD_PERSON_FRONT) {
			active = true;
			// The nearest diagonal to where the player looks, so that forward stays roughly up on the screen
			float look = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getYRot() : 0;
			yaw = Math.round((look - 45.0F) / 90.0F) * 90.0F + 45.0F;
			return CameraType.THIRD_PERSON_BACK;
		}
		if (current == CameraType.THIRD_PERSON_BACK && active) {
			active = false;
			return CameraType.FIRST_PERSON;
		}
		return null;
	}

	public static boolean isActive() {
		if (active && Minecraft.getInstance().options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
			active = false;
		}
		return active;
	}

	public static float yaw() {
		return yaw;
	}

	/** Frees the mouse on entering and takes it back on leaving, unless a menu has it by then. */
	public static void tick(Minecraft minecraft) {
		if (isActive() && minecraft.gui.screen() == null && minecraft.mouseHandler.isMouseGrabbed()) {
			minecraft.mouseHandler.releaseMouse();
			if (minecraft.player != null) {
				minecraft.player.setXRot(0);
			}
			mouseFreed = true;
		} else if (!active && mouseFreed) {
			mouseFreed = false;
			if (minecraft.gui.screen() == null) {
				minecraft.mouseHandler.grabMouse();
			}
		}
	}

	/** Turns the player towards where the keys point on the screen, and walks them straight ahead. */
	public static Vec2 walk(Vec2 keys, LocalPlayer player) {
		if (keys.lengthSquared() == 0) {
			return keys;
		}
		float heading = yaw - (float) Math.toDegrees(Math.atan2(keys.x, keys.y));
		player.setYRot(heading);
		player.setYHeadRot(heading);
		return new Vec2(0, keys.length());
	}

	/** A boat answers only to its turning and rowing keys: turns it towards where the keys point on the screen, and rows once it roughly faces there. */
	public static Input steer(Vec2 keys, Input presses, AbstractBoat boat) {
		if (keys.lengthSquared() == 0) {
			return presses;
		}
		float heading = yaw - (float) Math.toDegrees(Math.atan2(keys.x, keys.y));
		float off = Mth.wrapDegrees(heading - boat.getYRot());
		return new Input(Math.abs(off) < ROW_WITHIN, false, off < -TURN_WITHIN, off > TURN_WITHIN, presses.jump(), presses.shift(), presses.sprint());
	}

	/** Holding the middle button and moving the mouse turns the view. */
	public static void turn(double pixels) {
		yaw += (float) pixels * TURN_PER_PIXEL;
	}

	/** Aims with the cursor rather than the crosshair: what lies under it, if within the player's reach. */
	public static void pick(Minecraft minecraft) {
		Window window = minecraft.getWindow();
		double mouseX = minecraft.mouseHandler.xpos();
		if (minecraft.options.keyPickItem.isDown()) {
			turn(mouseX - lastMouseX);
		}
		lastMouseX = mouseX;
		aim(minecraft, minecraft.mouseHandler.xpos() / window.getScreenWidth(), minecraft.mouseHandler.ypos() / window.getScreenHeight());
	}

	/** {@code x} and {@code y} as fractions of the window. */
	public static void aim(Minecraft minecraft, double x, double y) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			return;
		}
		Camera camera = minecraft.gameRenderer.mainCamera();
		Window window = minecraft.getWindow();
		double halfHeight = Math.tan(Math.toRadians(camera.getFov()) / 2);
		double up = (1 - 2 * y) * halfHeight;
		double right = (2 * x - 1) * halfHeight * window.getScreenWidth() / window.getScreenHeight();
		Vec3 direction = new Vec3(camera.forwardVector())
				.add(new Vec3(camera.upVector()).scale(up))
				.add(new Vec3(camera.leftVector()).scale(-right))
				.normalize();
		Vec3 from = camera.position();
		Vec3 to = from.add(direction.scale(RAY));
		BlockHitResult block = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, from, end, new AABB(from, end).inflate(1), EntitySelector.CAN_BE_PICKED, from.distanceToSqr(end));
		target = entity != null ? entity.getLocation() : end;
		Vec3 eye = player.getEyePosition();
		if (entity != null && eye.distanceTo(entity.getLocation()) <= player.entityInteractionRange()) {
			minecraft.hitResult = entity;
			minecraft.crosshairPickEntity = entity.getEntity();
		} else {
			minecraft.crosshairPickEntity = null;
			minecraft.hitResult = block.getType() != HitResult.Type.MISS && entity == null && eye.distanceTo(block.getLocation()) <= player.blockInteractionRange()
					? block
					: BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end));
		}
	}

	/** On a click the player turns to the cursor, for what goes where they look: arrows, pearls, the arm's swing. */
	public static void faceCursor(LocalPlayer player) {
		if (target != null) {
			player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
		}
	}
}
