package com.cassonade.client.mixin;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;

import com.cassonade.client.ambience.Atmosphere;
import com.cassonade.client.ambience.MilkyWay;
import com.cassonade.client.ambience.MorningMist;

/** Deepens the sky's blue, hides what the sky holds behind the morning mist, and draws the Milky Way right after the stars, under their rotation and brightness. */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
	@Shadow
	@Final
	private RenderSystem.AutoStorageIndexBuffer quadIndices;
	@Unique
	private @Nullable GpuBuffer cassonade$milkyWayStars;
	@Unique
	private @Nullable GpuBuffer cassonade$milkyWayHaze;
	@Unique
	private @Nullable GpuBuffer cassonade$milkyWayBulge;
	@Unique
	private static final Vector3f WHITE = new Vector3f(1, 1, 1);

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void cassonade$deepenSky(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo ci) {
		state.skyColor = Atmosphere.sky(level, state.skyColor, partialTicks);
		if (MorningMist.density() > 0 && state.sunriseAndSunsetColor != null) {
			// All of this is drawn over the sky's fade: the mist has to hide it itself
			Vector4fc glow = state.sunriseAndSunsetColor;
			state.sunriseAndSunsetColor = new Vector4f(glow.x(), glow.y(), glow.z(), glow.w() * MorningMist.veil());
			state.rainBrightness *= MorningMist.veil();
			state.starBrightness *= MorningMist.veil();
		}
	}

	@Inject(method = "renderStars", at = @At("TAIL"))
	private void cassonade$drawMilkyWay(RenderPass renderPass, float starBrightness, PoseStack poseStack, CallbackInfo ci) {
		if (cassonade$milkyWayStars == null) {
			cassonade$milkyWayStars = MilkyWay.buildStars();
			cassonade$milkyWayHaze = MilkyWay.buildHaze();
			cassonade$milkyWayBulge = MilkyWay.buildBulge();
		}
		cassonade$draw(renderPass, cassonade$milkyWayHaze, MilkyWay.hazeCount(), starBrightness * MilkyWay.HAZE_BRIGHTNESS, MilkyWay.HAZE_COLOR, poseStack);
		cassonade$draw(renderPass, cassonade$milkyWayBulge, MilkyWay.bulgeCount(), starBrightness * MilkyWay.BULGE_BRIGHTNESS, MilkyWay.BULGE_COLOR, poseStack);
		cassonade$draw(renderPass, cassonade$milkyWayStars, MilkyWay.starCount(), starBrightness * MilkyWay.STAR_BRIGHTNESS, WHITE, poseStack);
	}

	@Unique
	private void cassonade$draw(RenderPass renderPass, GpuBuffer vertices, int stars, float brightness, Vector3f color, PoseStack poseStack) {
		int indexCount = MilkyWay.indexCount(stars);
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.mul(poseStack.last().pose());
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
				.writeTransform(new Matrix4f(modelView), new Vector4f(brightness * color.x, brightness * color.y, brightness * color.z, brightness));
		renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.STARS));
		RenderSystem.bindDefaultUniforms(renderPass);
		renderPass.setUniform("DynamicTransforms", transforms);
		renderPass.setVertexBuffer(0, vertices.slice());
		renderPass.setIndexBuffer(quadIndices.getBuffer(indexCount), quadIndices.type());
		renderPass.drawIndexed(indexCount, 1, 0, 0, 0);
		modelView.popMatrix();
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void cassonade$closeMilkyWay(CallbackInfo ci) {
		if (cassonade$milkyWayStars != null) {
			cassonade$milkyWayStars.close();
			cassonade$milkyWayHaze.close();
			cassonade$milkyWayBulge.close();
		}
	}
}
