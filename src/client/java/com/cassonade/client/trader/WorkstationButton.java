package com.cassonade.client.trader;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.map.Pings;
import com.cassonade.compass.Coordinates;
import com.cassonade.trader.WorkstationPayload;

/**
 * A button beside a villager's trades, showing the block it works at: a click closes the screen, turns the player towards
 * it and points it out with a ping only this player sees, to find which lectern is whose. Hidden without an answer from the server.
 */
public final class WorkstationButton extends Button.Plain {
	public static final int SIZE = 20;
	private static final int ICON_INSET = 2;
	private static @Nullable WorkstationPayload answer;

	private final int containerId;

	public WorkstationButton(int x, int y, int containerId) {
		super(x, y, SIZE, SIZE, Component.empty(), button -> ((WorkstationButton) button).point(), DEFAULT_NARRATION);
		this.containerId = containerId;
		visible = false;
	}

	public static void receive(Minecraft minecraft, WorkstationPayload payload) {
		answer = payload;
	}

	public void update(Minecraft minecraft) {
		BlockPos pos = answer != null && answer.containerId() == containerId ? answer.pos() : null;
		boolean shown = pos != null && minecraft.level != null;
		if (shown && !visible) {
			setTooltip(Tooltip.create(Component.translatable("gui.cassonade.workstation.tooltip", block(minecraft, pos).getHoverName(),
					Component.literal(Coordinates.of(pos)))));
		}
		visible = shown;
	}

	private static ItemStack block(Minecraft minecraft, BlockPos pos) {
		return new ItemStack(minecraft.level.getBlockState(pos).getBlock());
	}

	private void point() {
		Minecraft minecraft = Minecraft.getInstance();
		if (answer != null && minecraft.level != null && minecraft.gui.screen() instanceof MerchantScreen screen) {
			BlockPos pos = answer.pos();
			Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 1, 0);
			screen.onClose();
			minecraft.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
			Pings.show(minecraft, block(minecraft, pos).getHoverName().getString(), top);
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractContents(graphics, mouseX, mouseY, a);
		Minecraft minecraft = Minecraft.getInstance();
		if (answer != null && minecraft.level != null) {
			graphics.item(block(minecraft, answer.pos()), getX() + ICON_INSET, getY() + ICON_INSET);
		}
	}
}
