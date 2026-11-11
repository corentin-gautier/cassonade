package com.cassonade.settings;

import java.util.Locale;

/**
 * One feature the player can turn off from the settings screen (client/settings/SettingsScreen, opened from Mod Menu),
 * all on by default. Its name and tooltip are {@code setting.cassonade.<id>} and {@code setting.cassonade.<id>.tooltip}.
 */
public enum Setting {
	// Inventory
	SORTING(Category.INVENTORY, Side.CLIENT),
	STASH(Category.INVENTORY, Side.CLIENT),
	CONTAINER_SEARCH(Category.INVENTORY, Side.CLIENT),
	SHIFT_DRAG(Category.INVENTORY, Side.CLIENT),
	SCROLL_TRANSFER(Category.INVENTORY, Side.CLIENT),
	LOCKED_SLOTS(Category.INVENTORY, Side.SERVER),
	BUNDLE_DRAG(Category.INVENTORY, Side.CLIENT),
	SHULKER_CLICKS(Category.INVENTORY, Side.SERVER),
	CONTAINER_PREVIEWS(Category.INVENTORY, Side.CLIENT),
	FOOD_TOOLTIP(Category.INVENTORY, Side.CLIENT),
	FOOD_IN_HAND(Category.INVENTORY, Side.CLIENT),
	ENCHANTMENT_DESCRIPTIONS(Category.INVENTORY, Side.CLIENT),
	GEAR_COMPARISON(Category.INVENTORY, Side.CLIENT),
	ANVIL_PENALTY(Category.INVENTORY, Side.CLIENT),
	FUEL_TOOLTIP(Category.INVENTORY, Side.CLIENT),
	PICKED_UP_LIST(Category.INVENTORY, Side.CLIENT),
	NEW_ITEM_DOTS(Category.INVENTORY, Side.CLIENT),
	HOTBAR_ROWS(Category.INVENTORY, Side.CLIENT),
	HAND_REFILL(Category.INVENTORY, Side.SERVER),
	DURABILITY_WARNING(Category.INVENTORY, Side.CLIENT),
	BULK_CRAFTING(Category.INVENTORY, Side.SERVER),
	RECIPE_USES(Category.INVENTORY, Side.CLIENT),
	CHAIN_CRAFTING(Category.INVENTORY, Side.SERVER),
	INTERFACE_SOUNDS(Category.INVENTORY, Side.CLIENT),

	// Blocks and items
	RIGHT_CLICK_HARVEST(Category.BLOCKS, Side.SERVER),
	HOPPER_FILTER(Category.BLOCKS, Side.SERVER),
	COMPOSTER(Category.BLOCKS, Side.SERVER),
	COLUMN_BONE_MEAL(Category.BLOCKS, Side.SERVER),
	FAST_LEAF_DECAY(Category.BLOCKS, Side.SERVER),
	DOUBLE_DOORS(Category.BLOCKS, Side.SERVER),
	SWINGING_DOORS(Category.BLOCKS, Side.CLIENT),
	SAFE_BRIDGING(Category.BLOCKS, Side.CLIENT),
	LADDER_EXTEND(Category.BLOCKS, Side.SERVER),
	FLOWER_POT_SWAP(Category.BLOCKS, Side.SERVER),
	ITEM_FRAME_SHEARS(Category.BLOCKS, Side.SERVER),
	OPEN_BEHIND_LABELS(Category.BLOCKS, Side.SERVER),
	CAULDRON_WASHING(Category.BLOCKS, Side.SERVER),
	SNOW_SHOVEL(Category.BLOCKS, Side.SERVER),
	COBWEBS(Category.BLOCKS, Side.SERVER),
	ENDER_CHEST_DROP(Category.BLOCKS, Side.SERVER, Applies.ON_WORLD_LOAD),
	PAINTING_CHOICE(Category.BLOCKS, Side.SERVER),
	ARMOR_STAND_SWAP(Category.BLOCKS, Side.SERVER),
	ARMOR_STAND_POSES(Category.BLOCKS, Side.SERVER),
	ARMOR_STAND_ARMS(Category.BLOCKS, Side.SERVER),
	PATH_SPEED(Category.BLOCKS, Side.SERVER),
	NO_TRAMPLING(Category.BLOCKS, Side.SERVER),
	BERRY_BUSH_ARMOR(Category.BLOCKS, Side.SERVER),
	BOATS_ON_LAND(Category.BLOCKS, Side.SERVER),
	VEHICLE_EXIT(Category.BLOCKS, Side.SERVER),

	// Mobs and combat
	VILLAGER_LEADS(Category.MOBS, Side.SERVER),
	PLAYER_BEDS(Category.MOBS, Side.SERVER),
	SILENT_NAME_TAG(Category.MOBS, Side.SERVER),
	GOLEMS_AVOID_FIRE(Category.MOBS, Side.SERVER),
	PETS_AVOID_FIRE(Category.MOBS, Side.SERVER),
	SPARE_PETS(Category.MOBS, Side.SERVER),
	CAT_HABITS(Category.MOBS, Side.SERVER),
	CATS_HUNT_SPIDERS(Category.MOBS, Side.SERVER),
	BEES_STAY_ASHORE(Category.MOBS, Side.SERVER),
	BLAZE_RANGE(Category.MOBS, Side.SERVER, Applies.ON_RESTART),
	BABY_ZOMBIE_AIM(Category.MOBS, Side.SERVER),
	HIT_THROUGH_PLANTS(Category.MOBS, Side.CLIENT),
	MILDER_POISON(Category.MOBS, Side.SERVER),
	NETHERITE_SWORD(Category.MOBS, Side.SERVER, Applies.ON_RESTART),
	NETHERITE_PICKAXE(Category.MOBS, Side.SERVER),

	// Time and world
	LONGER_DAYS(Category.WORLD, Side.SERVER),
	MOONLIGHT(Category.WORLD, Side.SERVER, Applies.ON_WORLD_LOAD),
	PEACEFUL_FULL_MOON(Category.WORLD, Side.SERVER),
	FASTER_SLEEP(Category.WORLD, Side.SERVER),
	INSTANT_RESPAWN(Category.WORLD, Side.CLIENT),
	INSTANT_PORTAL(Category.WORLD, Side.SERVER),
	CRYING_OBSIDIAN_PORTAL(Category.WORLD, Side.SERVER, Applies.ON_WORLD_LOAD),

	// Enchanting
	ENCHANTMENT_PREVIEW(Category.ENCHANTING, Side.SERVER),
	ENCHANTMENT_REROLL(Category.ENCHANTING, Side.SERVER),
	ANVIL_NO_LIMIT(Category.ENCHANTING, Side.SERVER),
	FREE_RENAMING(Category.ENCHANTING, Side.SERVER),
	ENCHANTMENT_LEVELS(Category.ENCHANTING, Side.SERVER, Applies.ON_WORLD_LOAD),
	MENDING_INFINITY(Category.ENCHANTING, Side.SERVER, Applies.ON_WORLD_LOAD),

	// Interface
	MENU_SCREENS(Category.INTERFACE, Side.CLIENT),
	LOADING_MAP(Category.INTERFACE, Side.CLIENT),
	WINDOW_ANIMATION(Category.INTERFACE, Side.CLIENT),
	WINDOW_BLUR(Category.INTERFACE, Side.CLIENT),
	BOSS_BARS(Category.INTERFACE, Side.CLIENT),
	EFFECTS_HUD(Category.INTERFACE, Side.CLIENT),
	CELEBRATIONS(Category.INTERFACE, Side.CLIENT),
	INFO_PANELS(Category.INTERFACE, Side.CLIENT),
	WORKSTATION_BUTTON(Category.INTERFACE, Side.CLIENT),
	BOOK_TITLES(Category.INTERFACE, Side.CLIENT),
	NAMED_CONTAINERS(Category.INTERFACE, Side.CLIENT),
	NOTE_BLOCK_INFO(Category.INTERFACE, Side.CLIENT),
	JUKEBOX_SONG(Category.INTERFACE, Side.CLIENT),
	REDSTONE_POWER(Category.INTERFACE, Side.CLIENT),
	WAXED_COPPER(Category.INTERFACE, Side.CLIENT),
	FACING_PREVIEW(Category.INTERFACE, Side.CLIENT),
	DAMAGE_DIRECTION(Category.INTERFACE, Side.CLIENT),
	LOWER_FIRE(Category.INTERFACE, Side.CLIENT),
	LOWER_SHIELD(Category.INTERFACE, Side.CLIENT),
	BOOK_EDITING(Category.INTERFACE, Side.CLIENT),
	SIGN_EDITING(Category.INTERFACE, Side.CLIENT),
	CHAT_MENTIONS(Category.INTERFACE, Side.CLIENT),
	MOUSE_BACK(Category.INTERFACE, Side.CLIENT),
	HIDDEN_ENTITIES(Category.INTERFACE, Side.CLIENT),

	// Ambience
	FLOCKS(Category.AMBIENCE, Side.CLIENT),
	SONGBIRDS(Category.AMBIENCE, Side.CLIENT),
	BUTTERFLIES(Category.AMBIENCE, Side.CLIENT),
	SQUIRRELS(Category.AMBIENCE, Side.CLIENT),
	SCORPIONS(Category.AMBIENCE, Side.CLIENT),
	NIGHT_SKY(Category.AMBIENCE, Side.CLIENT),
	HEAT_LIGHTNING(Category.AMBIENCE, Side.CLIENT),
	STORMS(Category.AMBIENCE, Side.CLIENT),
	SAND_DRIFTS(Category.AMBIENCE, Side.SERVER),
	MORNING_MIST(Category.AMBIENCE, Side.CLIENT),
	RAIN_COLOR(Category.AMBIENCE, Side.CLIENT, Applies.ON_RESOURCE_RELOAD),
	RAIN_ON_GLASS(Category.AMBIENCE, Side.CLIENT),
	FLOATING_PARTICLES(Category.AMBIENCE, Side.CLIENT),
	SEA_LIFE(Category.AMBIENCE, Side.CLIENT),
	WAVES(Category.AMBIENCE, Side.CLIENT),
	WATER_SPLASHES(Category.AMBIENCE, Side.CLIENT),
	WILDLIFE_SOUNDS(Category.AMBIENCE, Side.CLIENT),
	DISTANT_THUNDER(Category.AMBIENCE, Side.CLIENT),
	MUFFLED_UNDER_ROOF(Category.AMBIENCE, Side.CLIENT),
	CAVE_ECHO(Category.AMBIENCE, Side.CLIENT),

	// Light, sky and sea
	COLORED_LIGHTS(Category.LIGHT_AND_SEA, Side.CLIENT, Applies.ON_RESOURCE_RELOAD),
	HELD_LIGHTS(Category.LIGHT_AND_SEA, Side.CLIENT),
	SKY_COLOR(Category.LIGHT_AND_SEA, Side.CLIENT),
	SEA_HORIZON(Category.LIGHT_AND_SEA, Side.CLIENT, Applies.ON_RESOURCE_RELOAD),
	SEA_SURFACE(Category.LIGHT_AND_SEA, Side.CLIENT, Applies.ON_RESTART),
	UNDERWATER_VIEW(Category.LIGHT_AND_SEA, Side.CLIENT),
	UNDERWATER_SOUND(Category.LIGHT_AND_SEA, Side.CLIENT),
	MURKY_WATER(Category.LIGHT_AND_SEA, Side.SERVER),

	// Structures and loot
	TOWNS(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	TAVERNS(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	TAVERN_EVENINGS(Category.STRUCTURES, Side.SERVER),
	TAVERN_KEEPER(Category.STRUCTURES, Side.CLIENT, Applies.ON_RESOURCE_RELOAD),
	LAMP_POSTS(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	STONE_VILLAGES(Category.STRUCTURES, Side.SERVER),
	HOUSE_FOUNDATIONS(Category.STRUCTURES, Side.SERVER),
	VILLAGE_NESTS_AND_POSTS(Category.STRUCTURES, Side.SERVER),
	MANSION_DECOR(Category.STRUCTURES, Side.SERVER),
	RICHER_LOOT(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	MONUMENT_TREASURE(Category.STRUCTURES, Side.SERVER),
	PIRATE_BOOTY(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	LIGHTHOUSES(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	LONE_DWELLINGS(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	MINERS_CAMPS(Category.STRUCTURES, Side.SERVER),
	MUSHROOM_PLANTS(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	FAIRY_RINGS(Category.STRUCTURES, Side.SERVER, Applies.ON_WORLD_LOAD),
	TALLER_TRUNKS(Category.STRUCTURES, Side.SERVER),

	// Recipes
	FAIR_RECIPES(Category.RECIPES, Side.SERVER, Applies.ON_WORLD_LOAD),
	UNCRAFT_RECIPES(Category.RECIPES, Side.SERVER, Applies.ON_WORLD_LOAD),
	CUTTER_MEMORY(Category.RECIPES, Side.SERVER);

	/** Where the setting is read: the server's file rules what changes the game for everyone on it. */
	public enum Side {
		/** Kept by the server, the integrated one in singleplayer, and sent to its players. */
		SERVER,
		/** Each player's own, whatever server they play on. */
		CLIENT
	}

	/** When a change takes effect. */
	public enum Applies {
		AT_ONCE,
		/** Data from the mod's pack (recipes, loot, enchantments, structures), read as the world loads. */
		ON_WORLD_LOAD,
		/** What the game sets up once as it starts (default attributes and components). */
		ON_RESTART,
		/** Shaders, textures and chunk meshes: the settings screen reloads the resources when one changes. */
		ON_RESOURCE_RELOAD
	}

	/** The settings screen's pages, in this order. */
	public enum Category {
		INVENTORY, BLOCKS, MOBS, WORLD, ENCHANTING, RECIPES, INTERFACE, AMBIENCE, LIGHT_AND_SEA, STRUCTURES;

		public String key() {
			return "setting.cassonade.category." + name().toLowerCase(Locale.ROOT);
		}
	}

	private final Category category;
	private final Side side;
	private final Applies applies;

	Setting(Category category, Side side) {
		this(category, side, Applies.AT_ONCE);
	}

	Setting(Category category, Side side, Applies applies) {
		this.category = category;
		this.side = side;
		this.applies = applies;
	}

	/** Its key in the settings file and the language files. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String key() {
		return "setting.cassonade." + id();
	}

	public Category category() {
		return category;
	}

	public Side side() {
		return side;
	}

	public Applies applies() {
		return applies;
	}

	public static Setting byId(String id) {
		try {
			return valueOf(id.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
