package com.cassonade.client.mixin;

import java.util.ArrayList;
import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import com.cassonade.Cassonade;
import com.cassonade.client.EnchantmentPreview;
import com.cassonade.client.map.SmallText;

/**
 * The enchanting table's tooltip lists every enchantment an option gives, not just one with a "...?".
 * The client works the list out the way the server does, from the seed it sends (see
 * EnchantmentSeedPayload), the costs and the item; the enchantments' registry and tags come with the world.
 * The book above the slots is drawn in isometric pixel art (tools/enchanting_book_textures.py) rather
 * than as vanilla's 3D model, and the options' glyphs are written in the colors of the wooden tiles
 * they sit on (tools/gui_style.py), a size smaller and centered in them. Their costs in levels are
 * written in the gold of their icons (textures/gui.png) rather than in vanilla's bright green.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
	// Keep in sync with tools/enchanting_book_textures.py
	@Unique
	private static final int BOOK_WIDTH = 36;
	@Unique
	private static final int BOOK_HEIGHT = 32;
	@Unique
	private static final int OPEN_FRAMES = 7;
	@Unique
	private static final int PAGE_FRAMES = 8;
	@Unique
	private static final Identifier[] OPEN_SPRITES = new Identifier[OPEN_FRAMES];
	@Unique
	private static final Identifier[] PAGE_SPRITES = new Identifier[PAGE_FRAMES];
	/** Glyphs rising from the open book, as from the table in the world. */
	@Unique
	private static final int GLYPHS = 3;
	@Unique
	private static final int GLYPH_COLOR = 0xC898EC;
	@Unique
	private static final long GLYPH_RISE_MILLIS = 2400;

	// Vanilla's colors of the glyphs, on its grey tiles, and the mod's on the wooden ones
	@Unique
	private static final int VANILLA_GLYPHS = 0xFF685E4A;
	@Unique
	private static final int VANILLA_GLYPHS_HOVERED = 0xFFFFFF80;
	@Unique
	private static final int VANILLA_GLYPHS_DISABLED = 0xFF342F25;
	@Unique
	private static final int GLYPHS_COLOR = 0xFFB89C96;
	@Unique
	private static final int GLYPHS_HOVERED = 0xFFECD6D0;
	@Unique
	private static final int GLYPHS_DISABLED = 0xFF6E5452;
	// Vanilla's green of the options' costs in levels, and the mod's in the gold of the level icons
	@Unique
	private static final int VANILLA_COST = 0xFF80FF20;
	@Unique
	private static final int VANILLA_COST_DISABLED = 0xFF407F10;
	@Unique
	private static final int COST_COLOR = 0xFFFFD76A;
	@Unique
	private static final int COST_DISABLED = 0xFF8C605D;
	/** Where vanilla writes the glyphs below the top of their tile, and the tile's height. */
	@Unique
	private static final int TILE_TOP = 2;
	@Unique
	private static final int TILE_HEIGHT = 19;
	/** Room between the glyphs and the frame of the level icon, on their left. */
	@Unique
	private static final int GLYPHS_INDENT = 2;

	static {
		for (int i = 0; i < OPEN_FRAMES; i++) {
			OPEN_SPRITES[i] = Cassonade.id("container/enchanting_book/open_" + i);
		}
		for (int i = 1; i < PAGE_FRAMES; i++) {
			PAGE_SPRITES[i] = Cassonade.id("container/enchanting_book/page_" + i);
		}
	}

	private EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"))
	private List<Component> cassonade$listAllEnchantments(List<Component> texts, @Local(name = "i") int option) {
		List<EnchantmentInstance> enchantments = EnchantmentPreview.of(minecraft, menu, option);
		// Without the seed from the server, the vanilla clue
		if (enchantments.isEmpty()) {
			return texts;
		}

		// In place of the clue, the first line
		List<Component> lines = new ArrayList<>();
		for (EnchantmentInstance enchantment : enchantments) {
			lines.add(Enchantment.getFullname(enchantment.enchantment(), enchantment.level()).copy().withStyle(ChatFormatting.WHITE));
		}
		lines.addAll(texts.subList(1, texts.size()));
		return lines;
	}

	/** The book in the box vanilla gives its model: closed, opening, then its pages turning. */
	@Redirect(method = "extractBook", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;book(Lnet/minecraft/client/model/object/book/BookModel;Lnet/minecraft/resources/Identifier;FFFIIII)V"))
	private void cassonade$drawIsometricBook(GuiGraphicsExtractor graphics, BookModel model, Identifier texture, float scale, float open,
			float flip, int x0, int y0, int x1, int y1) {
		int x = (x0 + x1 - BOOK_WIDTH) / 2;
		int y = (y0 + y1 - BOOK_HEIGHT) / 2;
		int frame = Math.round(open * (OPEN_FRAMES - 1));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OPEN_SPRITES[frame], x, y, BOOK_WIDTH, BOOK_HEIGHT);
		if (frame < OPEN_FRAMES - 1) {
			return;
		}
		int page = Math.round(Mth.frac(flip) * PAGE_FRAMES);
		if (page > 0 && page < PAGE_FRAMES) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_SPRITES[page], x, y, BOOK_WIDTH, BOOK_HEIGHT);
		}
		long now = Util.getMillis();
		for (int i = 0; i < GLYPHS; i++) {
			long time = now + i * GLYPH_RISE_MILLIS / GLYPHS;
			float rise = (time % GLYPH_RISE_MILLIS) / (float) GLYPH_RISE_MILLIS;
			// Each glyph rises from somewhere else across the pages
			int across = Math.floorMod(Mth.murmurHash3Mixer((int) (time / GLYPH_RISE_MILLIS) * 31 + i), 20);
			int alpha = (int) (Mth.sin(rise * Mth.PI) * 255);
			int gx = x + 8 + across;
			int gy = y + 14 - (int) (rise * 14);
			graphics.fill(gx, gy, gx + 1, gy + 1, ARGB.color(alpha, GLYPH_COLOR));
		}
	}

	/**
	 * The options' glyphs a GUI scale step smaller (see SmallText) and centered on the height of their
	 * tile, where vanilla writes them full size against its top, and a little further from the level icon.
	 */
	@Redirect(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)I"))
	private int cassonade$smallGlyphsOnWood(GuiGraphicsExtractor graphics, Font font, FormattedText text, int x, int y, int width, int color,
			boolean dropShadow) {
		List<FormattedCharSequence> lines = font.split(text, width);
		float scale = SmallText.scale();
		// The glyphs' height: 7 pixels, without the 2 of spacing below the line
		float height = (lines.size() * font.lineHeight - 2) * scale;
		// Vanilla writes 2 pixels below the tile's top; rounded to whole screen pixels, to stay sharp
		int guiScale = minecraft.getWindow().getGuiScale();
		float top = Math.round((y - TILE_TOP + (TILE_HEIGHT - height) / 2) * guiScale) / (float) guiScale;
		graphics.pose().pushMatrix();
		graphics.pose().translate(x + GLYPHS_INDENT, top);
		graphics.pose().scale(scale, scale);
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(font, lines.get(i), 0, i * font.lineHeight, glyphsOnWood(color), dropShadow);
		}
		graphics.pose().popMatrix();
		return y + lines.size() * font.lineHeight;
	}

	@ModifyArg(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"),
			index = 4)
	private int cassonade$goldCost(int color) {
		return switch (color) {
			case VANILLA_COST -> COST_COLOR;
			case VANILLA_COST_DISABLED -> COST_DISABLED;
			default -> color;
		};
	}

	@Unique
	private static int glyphsOnWood(int color) {
		return switch (color) {
			case VANILLA_GLYPHS -> GLYPHS_COLOR;
			case VANILLA_GLYPHS_HOVERED -> GLYPHS_HOVERED;
			case VANILLA_GLYPHS_DISABLED -> GLYPHS_DISABLED;
			default -> color;
		};
	}
}
