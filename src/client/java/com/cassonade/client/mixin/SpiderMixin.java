package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

import com.cassonade.client.ClimbingSpiders;

/** How far up a spider is turned on the client, eased each tick, and the wall it turns to (ClimbingSpiders). */
@Mixin(Spider.class)
abstract class SpiderMixin extends Monster implements ClimbingSpiders.Climber {
	@Unique
	private float cassonade$climb;
	@Unique
	private float cassonade$climbO;
	@Unique
	private float cassonade$wallYaw;

	private SpiderMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void cassonade$turnToWall(CallbackInfo ci) {
		if (!level().isClientSide()) {
			return;
		}
		Spider spider = (Spider) (Object) this;
		float wall = spider.isClimbing() && !isDeadOrDying() ? ClimbingSpiders.wallYaw(spider) : Float.NaN;
		if (!Float.isNaN(wall)) {
			// Turned at once to a new wall while still flat; along the same one as it goes round a corner
			cassonade$wallYaw = cassonade$climb == 0 ? wall : Mth.approachDegrees(cassonade$wallYaw, wall, 30);
		}
		cassonade$climbO = cassonade$climb;
		cassonade$climb = Mth.approach(cassonade$climb, Float.isNaN(wall) ? 0 : 1, ClimbingSpiders.TURN);
	}

	@Override
	public float cassonade$climb(float partialTicks) {
		return Mth.lerp(partialTicks, cassonade$climbO, cassonade$climb);
	}

	@Override
	public float cassonade$wallYaw() {
		return cassonade$wallYaw;
	}
}
