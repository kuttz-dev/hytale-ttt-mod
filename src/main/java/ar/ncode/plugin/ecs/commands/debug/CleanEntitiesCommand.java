package ar.ncode.plugin.ecs.commands.debug;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.model.GameModeState;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.ecs.system.GameModeSystem.*;

public class CleanEntitiesCommand extends AbstractAsyncCommand {

    public CleanEntitiesCommand() {
        super("clean", "Debug command to get the current component position.");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
        return CompletableFuture.runAsync(() -> {
            commandContext.sendMessage(Message.raw("Cleaning entities..."));
            executeSync(commandContext);
        });
    }

    protected void executeSync(@NonNullDecl CommandContext ctx) {
        World currentWorld = Universe.get().getWorld(TroubleInTrorkTownPlugin.currentInstance);
        if (currentWorld == null) {
            ctx.sendMessage(Message.raw("Error obtaining world"));
            return;
        }

        GameModeState gameModeState = gameModeStateForWorld.get(currentWorld.getWorldConfig().getUuid());
        if (gameModeState != null) {
            currentWorld.execute(() -> {
                removeGraveStones(gameModeState, currentWorld);
                removeCorpses(gameModeState);
                removeDroppedItems(currentWorld);
            });
        } else {
            ctx.sendMessage(Message.raw("Warning - Error obtaining game mode state for world"));
        }
    }

}
