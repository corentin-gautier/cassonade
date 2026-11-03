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

import com.cassonade.client.SkyBehindTerrain;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;

import com.cassonade.client.ambience.Atmosphere;
import com.cassonade.client.ambience.MilkyWay;
import com.cassonade.client.ambience.MorningMist;

/** Deepens the sky's blue, hides what the sky holds behind the morning mist, draws the Milky Way right after the stars, under their rotation and brightness, and a glow around the sun under water. */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
	@Shadow
	@Final
	private RenderSystem.AutoStorageIndexBuffer quadIndices;
	@Shadow
	@Final
	private GpuBuffer bottomSkyBuffer;
	@Shadow
	@Final
	private RenderTarget renderTarget;
	@Unique
	private @Nullable GpuBuffer cassonade$milkyWayStars;
	@Unique
	private @Nullable GpuBuffer cassonade$milkyWayHaze;
	@Unique
	private @Nullable GpuBuffer cassonade$milkyWayBulge;
	@Unique
	private @Nullable GpuBuffer cassonade$glow;
	@Unique
	private static final int DISC_SIDES = 32;
	@Unique
	private static final Vector3f WHITE = new Vector3f(1, 1, 1);
	/** Radius and opacity of each layer of the glow around the sun, the narrow ones stacking into a bright halo. */
	@Unique
	private static final float[][] GLOW_LAYERS = { { 22, 0.8F }, { 45, 0.5F }, { 90, 0.35F }, { 170, 0.25F } };

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

	/** Past the last chunks, and through far water, the sun and moon would show under the sea: they now set behind the arc where the drawn sea ends. */
	@Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V"))
	private void cassonade$closeHorizon(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci, @Local RenderPass renderPass) {
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		// The game's disc, 512 wide and 16 blocks down, laid on the sea and as wide as the world drawn, so that its edge is the circle where terrain.fsh ends the world
		Minecraft minecraft = Minecraft.getInstance();
		float aboveSea = Math.max((float) (minecraft.gameRenderer.mainCamera().position().y - minecraft.level.getSeaLevel()), 0.5F);
		float reach = (minecraft.options.getEffectiveRenderDistance() * 16 - 16) / 512.0F;
		modelView.scale(reach, aboveSea / 16.0F, reach);
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelView), new Vector4f(state.skyColor, 0.0F));
		renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.SKY));
		RenderSystem.bindDefaultUniforms(renderPass);
		renderPass.setUniform("DynamicTransforms", transforms);
		renderPass.setVertexBuffer(0, bottomSkyBuffer.slice());
		renderPass.draw(10, 1, 0, 0);
		modelView.popMatrix();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void cassonade$copySky(CallbackInfo ci) {
		SkyBehindTerrain.copy(renderTarget);
	}

	/** Seen from under water, the waves scatter the sun's light into a wide glow, drawn under the sun so that it stays a square. */
	@Inject(method = "renderSun", at = @At("HEAD"))
	private void cassonade$sunGlow(RenderPass renderPass, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
		if (Minecraft.getInstance().gameRenderer.mainCamera().getFluidInCamera() != FogType.WATER) {
			return;
		}
		if (cassonade$glow == null) {
			cassonade$glow = cassonade$buildDisc();
		}
		renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.SUNRISE_SUNSET));
		RenderSystem.bindDefaultUniforms(renderPass);
		for (float[] layer : GLOW_LAYERS) {
			cassonade$drawDisc(renderPass, poseStack, cassonade$glow, layer[0], rainBrightness * layer[1]);
		}
	}

	@Unique
	private static void cassonade$drawDisc(RenderPass renderPass, PoseStack poseStack, GpuBuffer disc, float radius, float alpha) {
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.mul(poseStack.last().pose());
		modelView.scale(radius, 1.0F, radius);
		renderPass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms()
				.writeTransform(new Matrix4f(modelView), new Vector4f(1.0F, 0.97F, 0.88F, alpha)));
		renderPass.setVertexBuffer(0, disc.slice());
		renderPass.draw(DISC_SIDES + 2, 1, 0, 0);
		modelView.popMatrix();
	}

	/** A disc of radius 1 facing down from the sky's height, opaque at its center and clear at its edge, wound as the sun's quad so that it is not culled. */
	@Unique
	private static GpuBuffer cassonade$buildDisc() {
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized((DISC_SIDES + 2) * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
			BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
			builder.addVertex(0.0F, 100.0F, 0.0F).setColor(ARGB.white(1.0F));
			for (int i = 0; i <= DISC_SIDES; i++) {
				float angle = i * Mth.TWO_PI / DISC_SIDES;
				builder.addVertex(Mth.cos(angle), 100.0F, Mth.sin(angle)).setColor(ARGB.white(0.0F));
			}
			try (MeshData mesh = builder.buildOrThrow()) {
				return RenderSystem.getDevice().createBuffer(() -> "Cassonade underwater sun", 32, mesh.vertexBuffer());
			}
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
		if (cassonade$glow != null) {
			cassonade$glow.close();
		}
	}
}
