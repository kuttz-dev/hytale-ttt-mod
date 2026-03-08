package ar.ncode.plugin.ecs.system.event.listener;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.model.PlayerComponents;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ar.ncode.plugin.model.enums.TranslationKey.DEAD_PLAYER_CHAT_PREFIX;

/**
 * Separates chat between alive and dead (spectator) players.
 * - Dead players can only see messages from other dead players
 * - Alive players can only see messages from alive players
 * <p>
 * Uses thread-safe spectator tracking to avoid world thread access issues.
 * <p>
 * Closes #8
 */
public class ChatListener {

    public static ChatListener INSTANCE = new ChatListener();

    public static ChatListener get() {
        return INSTANCE;
    }

    private static void addTagPrefixToMessage(PlayerChatEvent event, boolean isSenderDead, PlayerComponents player) {
        // Add [DEAD] prefix for dead component messages
        if (isSenderDead) {
            event.setFormatter((playerRef, msg) ->
                    Message.join(
                            Message.translation(DEAD_PLAYER_CHAT_PREFIX.get()).color(DEAD_PLAYER_CHAT_PREFIX.getMessageColor()),
                            Message.raw(" - "),
                            Message.raw(playerRef.getUsername() + ": " + msg)
                    )
            );
            return;
        }

        CustomRole currentRole = player.info().getCurrentRoundRole();
        if (currentRole == null) {
            return;
        }

        String publicRoleMessagesPrefix = currentRole.getPublicRoleMessagesPrefix();

        String guiColor;
        if (currentRole.getCustomGuiColor() == null || currentRole.getCustomGuiColor().isBlank()) {
            guiColor = currentRole.getRoleGroup().guiColor;
        } else {
            guiColor = currentRole.getCustomGuiColor();
        }

        if (publicRoleMessagesPrefix != null && !publicRoleMessagesPrefix.isEmpty()) {
            event.setFormatter((playerRef, msg) ->
                    Message.join(
                            Message.translation(publicRoleMessagesPrefix).color(guiColor),
                            Message.raw(" - "),
                            Message.translation("server.chat.playerMessage")
                                    .param("username", playerRef.getUsername())
                                    .param("message", msg)
                    )
            );
        }
    }

    public PlayerChatEvent accept(PlayerChatEvent event) {
        PlayerRef sender = event.getSender();
        var ref = sender.getReference();
        if (ref == null || !ref.isValid() || sender.getWorldUuid() == null) {
            return event;
        }

        Store<EntityStore> store = sender.getReference().getStore();
        World world = Universe.get().getWorld(sender.getWorldUuid());
        if (world == null) return event;

        CountDownLatch latch = new CountDownLatch(1);
        world.execute(() -> {
            try {
                var player = PlayerAccessors.getPlayerFrom(sender, store);
                if (player.isEmpty()) {
                    return;
                }

                // Check if sender is a spectator using thread-safe set
                boolean isSenderDead = player.get().info().isSpectator();

                // Filter targets to only include players with same alive/dead status
                List<PlayerRef> filteredTargets = event.getTargets().stream()
                        .filter(target -> {
                            if (target == null) return false;
                            var targetPlayer = PlayerAccessors.getPlayerFrom(target, store);
                            if (targetPlayer.isEmpty()) return false;

                            // Check if target is spectator using thread-safe set
                            boolean isTargetDead = targetPlayer.get().info().isSpectator();
                            return (isSenderDead == isTargetDead) || isTargetDead;

                        }).collect(Collectors.toList());

                event.setTargets(filteredTargets);
                addTagPrefixToMessage(event, isSenderDead, player.get());

            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        return event;
    }
}
