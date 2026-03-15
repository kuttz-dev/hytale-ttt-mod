package ar.ncode.plugin.accessors;

import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class PlayerRefAccessors {

	private static final String DUMMY_IDENTIFIER = "DummyConnection";
	private static final String DUMMY_HANDLER_CLASS = "DummyPacketHandler";

	private PlayerRefAccessors() {
	}

	public static boolean isDummyPlayer(PlayerRef playerRef) {
		if (playerRef == null) {
			return false;
		}

		PacketHandler packetHandler = playerRef.getPacketHandler();
		if (packetHandler == null) {
			return false;
		}

		String identifier = packetHandler.getIdentifier();
		if (DUMMY_IDENTIFIER.equals(identifier)) {
			return true;
		}

		return packetHandler.getClass().getName().contains(DUMMY_HANDLER_CLASS);
	}
}