package ar.ncode.plugin.packet.filter;

import ar.ncode.plugin.config.DebugConfig;
import ar.ncode.plugin.ui.pages.ScoreBoardPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.protocol.packets.window.CloseWindow;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Set;

public class GuiPacketsFilter implements PlayerPacketFilter {

	public static final Set<Integer> WINDOW_PACKETS = Set.of(CloseWindow.PACKET_ID, ClientOpenWindow.PACKET_ID,
			ChatMessage.PACKET_ID);
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	@Override
	public boolean test(PlayerRef playerRef, Packet packet) {
		if (!WINDOW_PACKETS.contains(packet.getId())) {
			return false;
		}

		LOGGER.atInfo().log(packet.getClass().getSimpleName());

		Ref<EntityStore> reference = playerRef.getReference();
		if (reference == null || playerRef.getWorldUuid() == null) {
			return false;
		}

		World world = Universe.get().getWorld(playerRef.getWorldUuid());
		if (world == null) {
			return false;
		}

		boolean result = false;
		if (ChatMessage.PACKET_ID == packet.getId() && packet instanceof ChatMessage chatMessage) {
			String message = chatMessage.message;
			if (message != null && message.startsWith("/gm ") && DebugConfig.INSTANCE.isEnableChangingGameMode()) {
				return false;
			} else if (message != null && message.startsWith("/gm ")) {
				result = true;
			}

		}

		world.execute(() -> {
			Player player = reference.getStore().getComponent(reference, Player.getComponentType());
			if (player == null) {
				return;
			}

			if (CloseWindow.PACKET_ID == packet.getId()) {
				player.getWindowManager().closeAllWindows(reference, reference.getStore());
//                component.getPageManager().setPage(reference, reference.getStore(), Page.None);

			} else if (ClientOpenWindow.PACKET_ID == packet.getId()) {
//				component.getPageManager().openCustomPage(reference, reference.getStore(), new ScoreBoardPage(refComponent, CustomPageLifetime.CanDismissOrCloseThroughInteraction));

			} else if (ChatMessage.PACKET_ID == packet.getId() && packet instanceof ChatMessage chatMessage) {
				String message = chatMessage.message;
				if (message != null && message.startsWith("/gm ")) {
					player.getPageManager().openCustomPage(reference, reference.getStore(), new ScoreBoardPage(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction));
				}
			}
		});

		return result;
	}
}
