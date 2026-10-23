package com.vanillasugar.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the wings of an elytra worn in the gear slot, over the chestplate, and hides the cape as an
 * elytra worn as a chestplate does. One worn as a chestplate already has its wings drawn by vanilla.
 */
public final class GearWingsLayer extends WingsLayer<AvatarRenderState, PlayerModel> {
	/** The elytra in the player's gear slot, set on the render state by AvatarRendererMixin. */
	public static final RenderStateDataKey<ItemStack> ELYTRA = RenderStateDataKey.create(() -> "vanillasugar:gear_elytra");

	private GearWingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityRendererProvider.Context context) {
		super(renderer, context.getModelSet(), context.getEquipmentRenderer());
	}

	public static void init() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new GearWingsLayer(avatar, context));
			}
		});
		LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(state -> elytra(state).isEmpty());
	}

	private static ItemStack elytra(AvatarRenderState state) {
		return state.getDataOrDefault(ELYTRA, ItemStack.EMPTY);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
		ItemStack elytra = elytra(state);
		if (elytra.isEmpty() || state.chestEquipment.has(DataComponents.GLIDER)) {
			return;
		}

		// Vanilla's layer draws the wings of the chest item, so it is lent the gear's elytra
		ItemStack chest = state.chestEquipment;
		state.chestEquipment = elytra;
		super.submit(poseStack, submitNodeCollector, lightCoords, state, yRot, xRot);
		state.chestEquipment = chest;
	}
}
