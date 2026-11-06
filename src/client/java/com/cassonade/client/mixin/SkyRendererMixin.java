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
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

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
import com.cassonade.client.ambience.DawnGlow;
import com.cassonade.client.dev.DevRendering;
import com.cassonade.client.ambience.MilkyWay;
import com.cassonade.client.ambience.MorningMist;
import com.cassonade.client.ambience.Storm;

/** Deepens the sky's blue, hides what the sky holds behind the morning mist, draws the Milky Way right after the stars, under their rotation and brightness, and a glow around the sun under water. */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
	@Shadow
	@Final
	private RenderSystem.AutoStorageIndexBuffer quadIndices;
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
	private @Nullable GpuBuffer cassonade$horizon;
	@Unique
	private @Nullable GpuBuffer cassonade$dawn;
	/** The glow as last drawn this frame, drawn again over the horizon's disc; null when there is none. */
	@Unique
	private @Nullable Matrix4f cassonade$glowPose;
	@Unique
	private @Nullable Vector4f cassonade$glowColor;
	/** How far the horizon's disc is drawn: any distance gives the same picture around the camera, and this one is nearer than the glow. */
	@Unique
	private static final float HORIZON_DISTANCE = 8;
	/** The height of still water's surface in its block. */
	@Unique
	private static final float WATER_SURFACE = 8 / 9.0F;
	@Unique
	private static final int DISC_SIDES = 32;
	@Unique
	private static final int HORIZON_SIDES = 64;
	@Unique
	private static final Vector3f WHITE = new Vector3f(1, 1, 1);
	/** Radius and opacity of each layer of the glow around the sun, the narrow ones stacking into a bright halo. */
	@Unique
	private static final float[][] GLOW_LAYERS = { { 22, 0.8F }, { 45, 0.5F }, { 90, 0.35F }, { 170, 0.25F } };

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void cassonade$deepenSky(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo ci) {
		if (DevRendering.vanilla()) {
			return;
		}
		state.skyColor = Atmosphere.sky(level, state.skyColor, partialTicks);
		float veil = MorningMist.veil() * Storm.veil();
		if (veil < 1 && state.sunriseAndSunsetColor != null) {
			// All of this is drawn over the sky's fade: the mist and the storms have to hide it themselves
			Vector4fc glow = state.sunriseAndSunsetColor;
			state.sunriseAndSunsetColor = new Vector4f(glow.x(), glow.y(), glow.z(), glow.w() * veil);
			state.rainBrightness *= veil;
			state.starBrightness *= veil;
		}
	}

	/** The game's glow, with its color and transforms, but DawnGlow's smooth shape: its fan showed straight edges and a crease on the night sky. */
	@Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
	private void cassonade$smoothGlow(RenderPass renderPass, PoseStack poseStack, float sunAngle, Vector4fc color, CallbackInfo ci) {
		if (DevRendering.vanilla()) {
			return;
		}
		ci.cancel();
		cassonade$glowPose = null;
		float alpha = color.w();
		if (alpha <= 0.001F) {
			return;
		}
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.XP, 90.0F);
		poseStack.rotateDegrees(Axis.ZP, (Mth.sin(sunAngle) < 0.0F ? 180.0F : 0.0F) + 90.0F);
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.mul(poseStack.last().pose());
		modelView.scale(1.0F, 1.0F, alpha);
		cassonade$glowPose = new Matrix4f(modelView);
		cassonade$glowColor = new Vector4f(color);
		modelView.popMatrix();
		poseStack.popPose();
		cassonade$drawGlow(renderPass, DawnGlow.PIPELINE);
	}

	@Unique
	private void cassonade$drawGlow(RenderPass renderPass, RenderPipeline pipeline) {
		if (cassonade$dawn == null) {
			cassonade$dawn = DawnGlow.build();
		}
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(cassonade$glowPose, new Vector4f(cassonade$glowColor));
		renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
		RenderSystem.bindDefaultUniforms(renderPass);
		renderPass.setUniform("DynamicTransforms", transforms);
		renderPass.setVertexBuffer(0, cassonade$dawn.slice());
		renderPass.draw(DawnGlow.VERTICES, 1, 0, 0);
	}

	/** Past the last chunks, and through far water, the sun and moon would show under the sea: they now set behind the arc where the drawn sea ends. */
	@Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V"))
	private void cassonade$closeHorizon(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci, @Local RenderPass renderPass) {
		if (DevRendering.vanilla()) {
			return;
		}
		if (cassonade$horizon == null) {
			cassonade$horizon = cassonade$buildHorizon();
		}
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		// A disc like the game's bottom sky, its edge seen where the circle on the sea at which terrain.fsh ends the world is seen.
		// Drawn around the camera, its size changes nothing on screen: it is drawn small, nearer than the glow, for DawnGlow.BELOW_HORIZON
		Minecraft minecraft = Minecraft.getInstance();
		// At the water's surface, 8/9 of the way up the block under sea level: at sea level itself, the disc showed above the sea's
		// edge, a row of its color between the setting sun and the water
		float aboveSea = Math.max((float) (minecraft.gameRenderer.mainCamera().position().y - (minecraft.level.getSeaLevel() - 1 + WATER_SURFACE)), 0.5F);
		float reach = minecraft.options.getEffectiveRenderDistance() * 16 - 16;
		float scale = HORIZON_DISTANCE / reach;
		modelView.scale(reach / 512.0F * scale, aboveSea / 16.0F * scale, reach / 512.0F * scale);
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelView), new Vector4f(state.skyColor, 0.0F));
		renderPass.setPipeline(RenderSystem.getCompiledPipeline(DawnGlow.HORIZON));
		RenderSystem.bindDefaultUniforms(renderPass);
		renderPass.setUniform("DynamicTransforms", transforms);
		renderPass.setVertexBuffer(0, cassonade$horizon.slice());
		renderPass.draw(HORIZON_SIDES + 2, 1, 0, 0);
		modelView.popMatrix();
		if (cassonade$glowPose != null) {
			cassonade$drawGlow(renderPass, DawnGlow.BELOW_HORIZON);
		}
	}

	/**
	 * The game's bottom sky, wound the same way, with HORIZON_SIDES sides instead of 8: an octagon showed its straight sides wherever
	 * it hid the dawn's glow and the stars under the horizon, and whole over a world still loading. Just around the circle, so that
	 * the sides never fall short of it.
	 */
	@Unique
	private static GpuBuffer cassonade$buildHorizon() {
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized((HORIZON_SIDES + 2) * DefaultVertexFormat.POSITION.getVertexSize())) {
			BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
			builder.addVertex(0.0F, -16.0F, 0.0F);
			float radius = 512.0F / Mth.cos(Mth.PI / HORIZON_SIDES);
			for (int i = 0; i <= HORIZON_SIDES; i++) {
				float angle = i * Mth.TWO_PI / HORIZON_SIDES - Mth.PI;
				builder.addVertex(-radius * Mth.cos(angle), -16.0F, radius * Mth.sin(angle));
			}
			try (MeshData mesh = builder.buildOrThrow()) {
				return RenderSystem.getDevice().createBuffer(() -> "Cassonade horizon", 32, mesh.vertexBuffer());
			}
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void cassonade$copySky(CallbackInfo ci) {
		SkyBehindTerrain.copy(renderTarget);
		if (!DevRendering.vanilla() && renderTarget.getDepthTexture() != null) {
			// The horizon's depth was only a mask for the glow: the frame starts from a cleared depth, as the game leaves it
			RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(renderTarget.getDepthTexture(), 0.0);
		}
	}

	/** Seen from under water, the waves scatter the sun's light into a wide glow, drawn under the sun so that it stays a square. */
	@Inject(method = "renderSun", at = @At("HEAD"))
	private void cassonade$sunGlow(RenderPass renderPass, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
		if (Minecraft.getInstance().gameRenderer.mainCamera().getFluidInCamera() != FogType.WATER || DevRendering.vanilla()) {
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
		// Too faint to show through water; drawn over the water's fog, its haze turned into dark squares in Snell's window
		if (DevRendering.vanilla() || Minecraft.getInstance().gameRenderer.mainCamera().getFluidInCamera() == FogType.WATER) {
			return;
		}
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
		if (cassonade$horizon != null) {
			cassonade$horizon.close();
		}
		if (cassonade$dawn != null) {
			cassonade$dawn.close();
		}
	}
}
