package ar.ncode.plugin.commands.debug;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.config.DebugConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class Debug extends AbstractCommandCollection {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public Debug() {
		super("debug", "Command to toggle the debug mode.");
		this.addSubCommand(new DebugToggleCommand());
		this.addSubCommand(new GetCurrentPositonCommand());
		this.addSubCommand(new MemoryDebugCommand());
//		this.addSubCommand(new SetCurrentRole());
		this.addSubCommand(new Info());
	}

	public static class DebugToggleCommand extends AbstractAsyncCommand {

		RequiredArg<String> configArg = withRequiredArg(
				"config", "The config to toggle debug mode for.",
				ArgTypes.STRING
		);

		public DebugToggleCommand() {
			super("toggle", "Toggles the debug mode.");
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				DebugConfig config = DebugConfig.INSTANCE;
				boolean newValue;
				String arg = configArg.get(ctx);
				switch (arg) {
					case "gamemode" -> {
						newValue = !config.isEnableChangingGameMode();
						config.setEnableChangingGameMode(newValue);
					}
					case "gravestones" -> {
						newValue = !config.isPersistentGraveStones();
						config.setPersistentGraveStones(newValue);
					}
					case "blocks" -> {
						newValue = !config.isCanPlaceAndDestroyBlocks();
						config.setCanPlaceAndDestroyBlocks(newValue);
					}
					default -> {
						ctx.sendMessage(Message.raw("Invalid config specified."));
						return;
					}
				}

				ctx.sendMessage(
						Message.raw(arg + " is now " + (newValue ? "enabled" : "disabled") + ".")
				);
			});
		}

	}

	public static class GetCurrentPositonCommand extends AbstractAsyncCommand {

		public GetCurrentPositonCommand() {
			super("get-position", "Debug command to get the current component position.");
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
					commandContext.sendMessage(Message.raw("Could not get component reference!"));
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

//	public static class SetCurrentRole extends AbstractAsyncCommand {
//
//		RequiredArg<String> roleArg = withRequiredArg(
//				"role", "The role to be set.",
//				ArgTypes.STRING
//		);
//
//		public SetCurrentRole() {
//			super("role", "Sets the current role");
//		}
//
//		@NonNullDecl
//		@Override
//		protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext ctx) {
//			return CompletableFuture.runAsync(() -> {
//				String role = roleArg.get(ctx);
//				RoleGroup desiredRole;
//				try {
//					desiredRole = RoleGroup.valueOf(role.toUpperCase());
//				} catch (Exception e) {
//					ctx.sendMessage(Message.translation("Desired role doest not exist"));
//					return;
//				}
//
//				if (!ctx.isPlayer() || ctx.senderAsPlayerRef() == null) {
//					ctx.sendMessage(Message.raw("Command can only be used by players."));
//					return;
//				}
//
//				Player player = ctx.senderAs(Player.class);
//				World world = player.getWorld();
//				world.execute(() -> {
//					Ref<EntityStore> ref = ctx.senderAsPlayerRef();
//					var playerInfo = ref.getStore().getComponent(ref, PlayerGameModeInfo.componentType);
//					var playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
//
//					if (playerInfo == null) {
//						ctx.sendMessage(Message.raw("Error"));
//						return;
//					}
//
//
//					if ("spectator".equalsIgnoreCase(desiredRole)) {
//						playerInfo.setSpectator(true);
//						SpectatorMode.setGameModeToSpectator(playerRef, ref);
//					}
//
//				});
//			});
//		}
//	}

	public static class Info extends AbstractAsyncCommand {

		public Info() {
			super("info", "Shows info about the player and the gmaemode");
		}

		@NonNullDecl
		@Override
		protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				Player player = ctx.senderAs(Player.class);
				World world = player.getWorld();
				world.execute(() -> {
					Ref<EntityStore> ref = ctx.senderAsPlayerRef();
					var playerInfo = ref.getStore().getComponent(ref, PlayerGameModeInfo.componentType);

					if (playerInfo == null) {
						ctx.sendMessage(Message.raw("Error"));
						return;
					}

					var gameState = TroubleInTrorkTownPlugin.gameModeStateForWorld.get(world.getWorldConfig().getUuid());

					String message = "Player info: " + playerInfo;
					ctx.sendMessage(Message.raw(message));
					LOGGER.atInfo().log(message);


					message = "Game mode state: " + gameState;
					ctx.sendMessage(Message.raw(message));
				});
			});
		}
	}

}
