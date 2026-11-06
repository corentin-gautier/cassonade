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

/**
 * The game's own rendering, to compare with the mod's, in runClient only: while on, the mod's shaders are hidden from the resource packs (MultiPackResourceManagerMixin) and everything the mod changes in the
 * sky, fog, far terrain and colored light stands aside (each checks {@link #vanilla()}). Toggled by a key (DevKeys),
 * which reloads the resources, as the shaders and every chunk have to be built again.
 */
public final class DevRendering {
	private static volatile boolean vanilla;

	private DevRendering() {
	}

	/** Whether the mod's rendering stands aside this frame. */
	public static boolean vanilla() {
		return vanilla;
	}

	/** The game's shaders the mod replaces, hidden from every pack but the game's own while vanilla. */
	public static boolean hides(Identifier location) {
		return vanilla && location.getNamespace().equals("minecraft")
				&& location.getPath().startsWith("shaders/");
	}

	/** A pack without the hidden resources: every pack but the game's own, while vanilla. */
	public static PackResources filter(PackResources pack) {
		return vanilla && !pack.packId().equals("vanilla") ? new Filtered(pack) : pack;
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
