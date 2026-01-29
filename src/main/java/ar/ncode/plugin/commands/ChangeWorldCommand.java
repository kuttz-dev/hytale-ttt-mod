package ar.ncode.plugin.commands;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.TimeUnit;

public class ChangeWorldCommand extends CommandBase {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	RequiredArg<String> targetWorld = this.withRequiredArg("targetWorld", "Target world name", ArgTypes.STRING);

	public ChangeWorldCommand() {
		super("change-world", "Debug command to change the current world.");
	}

	/**
	 * Loads a new world instance and teleports all players from the current world to it.
	 * Note: This method does NOT check the transition flag - callers should use
	 * scheduleWorldTransition() from PlayerReadyEventListener to ensure proper timing.
	 *
	 * @param currentWorld The world to teleport players from
	 * @param newWorldName The name of the world template to spawn
	 */
	public static void loadInstance(World currentWorld, String newWorldName) {
		currentWorld.execute(() -> {
			TroubleInTrorkTownPlugin.currentInstance = newWorldName;
			World targetWorld = null;
			try {
				targetWorld = InstancesPlugin.get()
						.spawnInstance(newWorldName, currentWorld, new Transform())
						.get();
			} catch (Exception e) {
				LOGGER.atSevere().withCause(e).log("Failed to spawn instance: " + newWorldName);
				throw new RuntimeException(e);
			}

			for (PlayerRef playerRef : currentWorld.getPlayerRefs()) {
				Ref<EntityStore> ref = playerRef.getReference();
				PlayerGameModeInfo playerInfo = ref.getStore().getComponent(ref, PlayerGameModeInfo.componentType);

				if (ref == null || !ref.isValid()) {
					continue;
				}

				playerInfo.setWorldInstance(newWorldName);
				InstancesPlugin.teleportPlayerToInstance(
						ref,
						ref.getStore(),
						targetWorld,
						null
				);
			}
		});
	}

	@Override
	protected void executeSync(@NonNullDecl CommandContext commandContext) {
		World originWorld = commandContext.senderAs(Player.class).getWorld();
		String[] targetWorldName = commandContext.getInput(targetWorld);

		if (targetWorldName.length != 1) {
			commandContext.sendMessage(Message.raw("You must specify a target world name."));
			return;
		}

		if (originWorld == null) {
			commandContext.sendMessage(Message.raw("You are not in a world."));
			return;
		}

		loadInstance(originWorld, targetWorldName[0]);
	}
}
