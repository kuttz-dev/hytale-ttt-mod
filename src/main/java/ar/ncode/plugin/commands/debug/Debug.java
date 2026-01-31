package ar.ncode.plugin.commands.debug;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;

public class Debug extends AbstractCommandCollection {

	public Debug() {
		super("debug", "Command to toggle the debug mode.");
		this.addSubCommand(new DebugToggleCommand());
		this.addSubCommand(new GetCurrentPositonCommand());
		this.addSubCommand(new MemoryDebugCommand());
	}

	public static class DebugToggleCommand extends AbstractAsyncCommand {

		public DebugToggleCommand() {
			super("toggle", "Toggles the debug mode.");
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				boolean newDebugMode = !config.get().isDebugMode();
				config.get().setDebugMode(newDebugMode);
				config.save();
				ctx.sendMessage(Message.raw("Debug mode is now " + (newDebugMode ? "enabled" : "disabled") + "."));
			});
		}

	}

	public static class GetCurrentPositonCommand extends AbstractAsyncCommand {

		public GetCurrentPositonCommand() {
			super("get-position", "Debug command to get the current player position.");
		}

		@NonNullDecl
		@Override
		protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
			return CompletableFuture.runAsync(() -> {
				if (!commandContext.isPlayer()) {
					commandContext.sendMessage(Message.raw("This command can only be used by players."));
					return;
				}

				var player = commandContext.senderAs(Player.class);
				Ref<EntityStore> reference = player.getReference();

				if (reference == null || !reference.isValid()) {
					commandContext.sendMessage(Message.raw("Could not get player reference!"));
					return;
				}

				player.getWorld().execute(() -> {
					TransformComponent transform = reference.getStore().getComponent(reference, TransformComponent.getComponentType());
					if (transform == null) {
						player.sendMessage(Message.raw("Could not get position!"));
						return;
					}

					Vector3d position = transform.getPosition();
					double x = position.getX();
					double y = position.getY();
					double z = position.getZ();

					player.sendMessage(Message.raw(String.format("Position: %.1f, %.1f, %.1f", x, y, z)));
				});
			});
		}
	}

}
