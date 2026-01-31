package ar.ncode.plugin.system.event.listener;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Separates chat between alive and dead (spectator) players.
 * - Dead players can only see messages from other dead players
 * - Alive players can only see messages from alive players
 *
 * Uses thread-safe spectator tracking to avoid world thread access issues.
 *
 * Closes #8
 */
public class DeadChatListener implements Consumer<PlayerChatEvent> {

    @Override
    public void accept(PlayerChatEvent event) {
        PlayerRef sender = event.getSender();
        if (sender == null) return;

        // Check if sender is a spectator using thread-safe set
        boolean isSenderDead = TroubleInTrorkTownPlugin.spectatorPlayers.contains(sender.getUuid());

        // Filter targets to only include players with same alive/dead status
        List<PlayerRef> filteredTargets = event.getTargets().stream()
            .filter(target -> {
                if (target == null) return false;

                // Check if target is spectator using thread-safe set
                boolean isTargetDead = TroubleInTrorkTownPlugin.spectatorPlayers.contains(target.getUuid());
                return isSenderDead == isTargetDead;
            })
            .collect(Collectors.toList());

        event.setTargets(filteredTargets);

        // Add [DEAD] prefix for dead player messages
        if (isSenderDead) {
            event.setFormatter((playerRef, msg) ->
                Message.join(
                    Message.raw("[DEAD] ").color("#888888"),
                    Message.raw(playerRef.getUsername() + ": " + msg)
                )
            );
        }
    }
}
