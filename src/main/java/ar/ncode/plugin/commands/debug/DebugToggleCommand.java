package ar.ncode.plugin.commands.debug;

import ar.ncode.plugin.config.DebugConfig;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class DebugToggleCommand extends AbstractCommandCollection {

	public DebugToggleCommand() {
		super("toggle", "Toggles different configurations in debug mode.");
		addSubCommand(new ToggleGamemodeCommand());
		addSubCommand(new ToggleBlockPlacementCommand());
		addSubCommand(new TogglePersistentGravestonesCommand());
		addSubCommand(new ToggleEntitiesPersistenceCommand());
	}

	public static class ToggleGamemodeCommand extends AbstractAsyncCommand {

		public ToggleGamemodeCommand() {
			super("gamemode", "Enable / Disable game mode changing for OP users");
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				DebugConfig config = DebugConfig.INSTANCE;
				boolean newValue = !config.isEnableChangingGameMode();
				config.setEnableChangingGameMode(newValue);
				ctx.sendMessage(
						Message.raw("game mode changing is now " + (newValue ? "enabled" : "disabled") + ".")
				);
			});
		}
	}

	public static class TogglePersistentGravestonesCommand extends AbstractAsyncCommand {

		public TogglePersistentGravestonesCommand() {
			super("gravestones", "Enable / Disable gravestones persistence after rounds");
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				DebugConfig config = DebugConfig.INSTANCE;
				boolean newValue = !config.isPersistentGraveStones();
				config.setPersistentGraveStones(newValue);
				ctx.sendMessage(
						Message.raw("gravestones persistence is now " + (newValue ? "enabled" : "disabled") + ".")
				);
			});
		}
	}

	public static class ToggleBlockPlacementCommand extends AbstractAsyncCommand {

		public ToggleBlockPlacementCommand() {
			super("blocks", "Enable / Disable block placement");
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				DebugConfig config = DebugConfig.INSTANCE;
				boolean newValue = !config.isCanPlaceAndDestroyBlocks();
				config.setCanPlaceAndDestroyBlocks(newValue);
				ctx.sendMessage(
						Message.raw("block placement is now " + (newValue ? "enabled" : "disabled") + ".")
				);
			});
		}
	}

	public static class ToggleEntitiesPersistenceCommand extends AbstractAsyncCommand {

		public ToggleEntitiesPersistenceCommand() {
			super("entities", "Enable / Disable entities persistence after rounds");
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				DebugConfig config = DebugConfig.INSTANCE;
				boolean newValue = !config.entitiesShouldDisappearAfterRound();
				config.entitiesShouldDisappearAfterRound(newValue);
				ctx.sendMessage(
						Message.raw("entities should disappear after round: " + (newValue ? "true" : "false") + ".")
				);
			});
		}
	}

}
