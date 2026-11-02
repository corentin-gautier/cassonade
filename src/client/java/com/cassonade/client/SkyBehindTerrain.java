package com.cassonade.client;

import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;

/**
 * Copies of the sky as it was just drawn. Far terrain takes the color of what lies behind it, as the sky is not one color
 * that a fog could give. Under water the sky leaves the screen and only shows in the sea's surface, seen from below.
 * A copy is transparent when it does not apply this frame.
 */
public final class SkyBehindTerrain {
	private static final Vector4f NONE = new Vector4f();
	private static final Vector4f clearColor = new Vector4f();
	private static @Nullable GpuTexture behind;
	private static @Nullable GpuTextureView behindView;
	private static @Nullable GpuTexture above;
	private static @Nullable GpuTextureView aboveView;

	private SkyBehindTerrain() {
	}

	public static void beginFrame(Vector4fc frameClearColor) {
		clearColor.set(frameClearColor);
		RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		if (behind == null || behind.getWidth(0) != target.width || behind.getHeight(0) != target.height) {
			if (behind != null) {
				behindView.close();
				behind.close();
				aboveView.close();
				above.close();
			}
			behind = create("Cassonade sky behind terrain", target);
			behindView = RenderSystem.getDevice().createTextureView(behind);
			above = create("Cassonade sky above water", target);
			aboveView = RenderSystem.getDevice().createTextureView(above);
		}
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.clearColorTexture(behind, NONE);
		encoder.clearColorTexture(above, NONE);
	}

	private static GpuTexture create(String label, RenderTarget target) {
		return RenderSystem.getDevice().createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
				target.getColorTexture().getFormat(), target.width, target.height, 1, 1);
	}

	public static void copy(RenderTarget target) {
		if (behind == null || behind.getWidth(0) != target.width || behind.getHeight(0) != target.height) {
			return;
		}
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		if (Minecraft.getInstance().gameRenderer.mainCamera().getFluidInCamera() == FogType.WATER) {
			encoder.copyTextureToTexture(target.getColorTexture(), above, 0, 0, 0, 0, 0, target.width, target.height);
			encoder.clearColorTexture(target.getColorTexture(), clearColor);
		} else {
			encoder.copyTextureToTexture(target.getColorTexture(), behind, 0, 0, 0, 0, 0, target.width, target.height);
		}
	}

	public static void bind(RenderPass renderPass) {
		renderPass.setUniform("SkySampler", behindView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
		renderPass.setUniform("SkyAboveSampler", aboveView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
	}
}
