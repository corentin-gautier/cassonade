package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import com.cassonade.client.quest.QuestLog;

/** The quest journal's offer in the tests: declined on joining any world (see the mixin beside), except for the test of the offer. */
public final class JournalOffer {
	public static volatile boolean shown;

	private JournalOffer() {
	}

	/** Takes up the journal instead, for a test of what it shows. */
	static void follow(ClientGameTestContext context) {
		context.waitFor(client -> QuestLog.hasChosen());
		context.runOnClient(client -> QuestLog.setEnabled(true));
	}
}
