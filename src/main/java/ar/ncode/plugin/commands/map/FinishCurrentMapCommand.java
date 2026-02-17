package ar.ncode.plugin.commands.map;

import ar.ncode.plugin.system.event.FinishCurrentMapEvent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static ar.ncode.plugin.commands.spawn.ShowSpawnPoints.getWorldFromCommandContext;
import static ar.ncode.plugin.model.CustomPermissions.TTT_MAP_FINISH;

public class FinishCurrentMapCommand extends AbstractAsyncCommand {

	public FinishCurrentMapCommand() {
		super("finish", "description");
		addAliases("end");
		requirePermission(TTT_MAP_FINISH);
	}

	@NonNullDecl
	@Override
	protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
		World world = getWorldFromCommandContext(commandContext);

		if (world == null) {
			return CompletableFuture.runAsync(() -> {
				commandContext.sendMessage(Message.raw("Could not get world."));
			});
		}

		return HytaleServer.get().getEventBus()
				.dispatchForAsync(FinishCurrentMapEvent.class)
				.dispatch(new FinishCurrentMapEvent(world.getWorldConfig().getUuid()))
				.thenAccept(_ -> commandContext.sendMessage(Message.raw("Finished current map.")));
	}
}
