package ar.ncode.plugin.commands.spawn;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.SpawnPoint;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class AddSpawnPointCommand extends AbstractAsyncCommand {
	public AddSpawnPointCommand() {
		super("add", "Add spawn point for player.");
	}

	@NonNullDecl
	@Override
	protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext ctx) {
		return CompletableFuture.runAsync(() -> {
			Ref<EntityStore> reference = ctx.senderAsPlayerRef();

			if (reference == null || !reference.isValid()) {
				ctx.sendMessage(Message.raw("You can't use this command from the console."));
				return;
			}

			var world = reference.getStore().getExternalData().getWorld();
			world.execute(() -> {
				var transformComponent = reference.getStore().getComponent(reference, TransformComponent.getComponentType());
				if (transformComponent == null) {
					ctx.sendMessage(Message.raw("An error occurred while trying to access your player information."));
					return;
				}

				// Here you would add the logic to actually store the loot position
				SpawnPoint spawnPoint = new SpawnPoint(
						transformComponent.getPosition().clone(),
						transformComponent.getRotation().clone()
				);

				String worldName = world.getWorldConfig().getDisplayName().replace(" ", "_").toLowerCase();
				InstanceConfig instanceConfig = TroubleInTrorkTownPlugin.instanceConfig.get(worldName).get();
				SpawnPoint[] playerSpawnPoints = instanceConfig.getPlayerSpawnPoints();
				if (playerSpawnPoints == null) {
					playerSpawnPoints = new SpawnPoint[0];
				}

				SpawnPoint[] newPlayerSpawnPoints = new SpawnPoint[playerSpawnPoints.length + 1];
				System.arraycopy(playerSpawnPoints, 0, newPlayerSpawnPoints, 0, playerSpawnPoints.length);
				newPlayerSpawnPoints[playerSpawnPoints.length] = spawnPoint;
				instanceConfig.setPlayerSpawnPoints(newPlayerSpawnPoints);

				TroubleInTrorkTownPlugin.instanceConfig.get(worldName).save();

				ctx.sendMessage(Message.raw("Spawn position added at your current location."));
			});
		});
	}
}
