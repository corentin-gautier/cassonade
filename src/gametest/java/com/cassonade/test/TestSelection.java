package com.cassonade.test;

import java.util.Arrays;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;

/**
 * Which client tests run: all of them, or only those named with -Ptests (see build.gradle), by their
 * class name without "ClientGameTest": {@code ./gradlew runClientGameTest -Ptests=Gear,VanillaServer}.
 */
final class TestSelection {
	private TestSelection() {
	}

	static boolean skips(FabricClientGameTest test) {
		String names = System.getProperty("cassonade.tests");
		if (names == null || names.isBlank()) {
			return false;
		}
		String name = test.getClass().getSimpleName().replace("ClientGameTest", "");
		return Arrays.stream(names.split(",")).map(String::trim).noneMatch(name::equalsIgnoreCase);
	}
}
