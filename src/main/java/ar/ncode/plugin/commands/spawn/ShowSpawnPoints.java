package ar.ncode.plugin.commands.spawn;

import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.config.instance.SpawnPoint;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.CompletableFuture;

import static ar.ncode.plugin.commands.loot.LootShowSpawnPointsCommand.addBlockToWorldOnSpawnPoint;
import static ar.ncode.plugin.system.DeathSystem.setBlockWithRotation;

public class ShowSpawnPoints extends AbstractAsyncCommand {

	public ShowSpawnPoints() {
		super("show", "Show all spawn points in the world with statues.");
	}

	private static void spawnStatue(@NonNullDecl CommandContext commandContext, WorldChunk worldChunk, Vector3i position, int rotationIndex) {
		boolean result = setBlockWithRotation(worldChunk,
				position.x, position.y, position.z,
				"Furniture_Ancient_Statue", rotationIndex
		);

		if (!result) {
			commandContext.sendMessage(Message.raw("Could not set statue at spawn point: " + position));
		}
	}

	@NullableDecl
	public static World getWorldFromCommandContext(@NonNullDecl CommandContext commandContext) {
		if (!commandContext.isPlayer()) {
			commandContext.sendMessage(Message.raw("This command can only be used by players."));
			return null;
		}

		var player = commandContext.senderAs(Player.class);
		Ref<EntityStore> reference = player.getReference();

		if (reference == null || !reference.isValid()) {
			commandContext.sendMessage(Message.raw("Could not get component reference!"));
			return null;
		}

		World world = player.getWorld();

		if (world == null) {
			commandContext.sendMessage(Message.raw("Could not get world from component!"));
			return null;
		}

		return world;
	}

	@NonNullDecl
	@Override
	protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
		return CompletableFuture.runAsync(() -> {
			World world = getWorldFromCommandContext(commandContext);
			if (world == null) return;

			world.execute(() -> {

				var instanceConfig = WorldAccessors.getWorldInstanceConfig(world);
				SpawnPoint[] playerSpawnPoints = instanceConfig.getPlayerSpawnPoints();

				for (SpawnPoint spawnPoint : playerSpawnPoints) {
					boolean result = addBlockToWorldOnSpawnPoint(commandContext, spawnPoint, world, "Furniture_Ancient_Statue");

					if (!result) {
						commandContext.sendMessage(Message.raw("Could not set statue at spawn point: " + spawnPoint.getPosition().toVector3i()));
					}
				}

				commandContext.sendMessage(Message.raw("All spawn points have been shown with statues."));
			});
		});
	}
}
