package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.cassonade.stable.HitchingPostBlock;
import com.cassonade.stable.Stables;

/**
 * A lead's knot holds on a hitching post as on a fence, the post being no fence so that fences do not join onto it; and
 * it is tied around the end of the rail, rather than at the middle, where it would sit inside the thick post: the lead
 * starts from there, and a click on the post opens it rather than untying the knot.
 */
@Mixin(LeashFenceKnotEntity.class)
abstract class LeashFenceKnotEntityMixin {
	/**
	 * From the block's middle to the ring drawn around the rail's end (client/stable/PostKnot), 3 pixels from the end,
	 * away from the coil of rope on its other end.
	 */
	private static final double RAIL_END = 5 / 16.0;
	/** Up from where a knot sits on a fence, so the lead, held 0.2 above, starts from the rail's middle, 12.5 pixels up. */
	private static final double RAIL_RISE = 12.5 / 16 - 0.2 - 0.375;

	@Inject(method = "survives", at = @At("RETURN"), cancellable = true)
	private void cassonade$onHitchingPost(CallbackInfoReturnable<Boolean> cir) {
		LeashFenceKnotEntity knot = (LeashFenceKnotEntity) (Object) this;
		if (!cir.getReturnValueZ() && knot.level().getBlockState(knot.getPos()).is(Stables.BLOCK)) {
			cir.setReturnValue(true);
		}
	}

	/** Still in the post's block, so the knot is found and saved by it as usual. */
	@Inject(method = "recalculateBoundingBox", at = @At("TAIL"))
	private void cassonade$atRailEnd(CallbackInfo ci) {
		LeashFenceKnotEntity knot = (LeashFenceKnotEntity) (Object) this;
		BlockState state = knot.level().getBlockState(knot.getPos());
		if (!state.is(Stables.BLOCK)) {
			return;
		}
		Direction end = state.getValue(HitchingPostBlock.FACING).getCounterClockWise();
		knot.setPosRaw(knot.getX() + end.getStepX() * RAIL_END, knot.getY() + RAIL_RISE, knot.getZ() + end.getStepZ() * RAIL_END);
		double halfWidth = knot.getType().getWidth() / 2.0;
		knot.setBoundingBox(new AABB(knot.getX() - halfWidth, knot.getY(), knot.getZ() - halfWidth, knot.getX() + halfWidth,
				knot.getY() + knot.getType().getHeight(), knot.getZ() + halfWidth));
	}
}
