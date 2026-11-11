package com.cassonade.client.dev;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * The game's own rendering, to compare with the mod's, in runClient only: while on, the mod's shaders are hidden from the resource packs (MultiPackResourceManagerMixin) and everything the mod changes in the
 * sky, fog, far terrain and colored light stands aside (each checks {@link #vanilla()}). Toggled by a key (DevKeys),
 * which reloads the resources, as the shaders and every chunk have to be built again.
 * <p>
 * The far sea's setting hides the same shaders: the code feeding them follows the setting as it was at the last reload
 * ({@link #seaHorizon()}), so shaders and code never disagree; the settings screen reloads the resources on a change.
 */
public final class DevRendering {
	private static volatile boolean vanilla;
	private static volatile boolean seaShaders = true;

	private DevRendering() {
	}

	/** Whether the mod's rendering stands aside this frame. */
	public static boolean vanilla() {
		return vanilla;
	}

	/** As the resources reload: whether the far sea's shaders are loaded this time. */
	public static void reloading() {
		seaShaders = Settings.on(Setting.SEA_HORIZON);
	}

	/** Whether the far sea's shaders, and what draws for them (the horizon, the sky behind terrain), are in use. */
	public static boolean seaHorizon() {
		return !vanilla && seaShaders;
	}

	/** Whether the mod's shaders are left out of the packs. */
	public static boolean hidesShaders() {
		return vanilla || !seaShaders;
	}

	/** The game's shaders the mod replaces, hidden from every pack but the game's own while vanilla. */
	public static boolean hides(Identifier location) {
		return hidesShaders() && location.getNamespace().equals("minecraft")
				&& location.getPath().startsWith("shaders/");
	}

	/** A pack without the hidden resources: every pack but the game's own, while vanilla. */
	public static PackResources filter(PackResources pack) {
		return hidesShaders() && !pack.packId().equals("vanilla") ? new Filtered(pack) : pack;
	}

	private record Filtered(PackResources pack) implements PackResources {
		@Override
		public @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier location) {
			return hides(location) ? null : pack.getResource(type, location);
		}

		@Override
		public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
			pack.listResources(type, namespace, directory, (location, resource) -> {
				if (!hides(location)) {
					output.accept(location, resource);
				}
			});
		}

		@Override
		public Set<String> getNamespaces(PackType type) {
			return pack.getNamespaces(type);
		}

		@Override
		public PackLocationInfo location() {
			return pack.location();
		}

		@Override
		public @Nullable IoSupplier<InputStream> getRootResource(String... path) {
			return pack.getRootResource(path);
		}

		@Override
		public <T> @Nullable T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException {
			return pack.getMetadataSection(metadataSerializer);
		}

		@Override
		public void close() {
			pack.close();
		}
	}

	public static CompletableFuture<Void> toggle(Minecraft minecraft) {
		vanilla = !vanilla;
		minecraft.gui.hud.setOverlayMessage(Component.literal(vanilla ? "Vanilla rendering" : "Cassonade rendering"), false);
		return minecraft.reloadResourcePacks();
	}
}
