package ar.ncode.plugin.commands.loot;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.SpawnPoint;
import ar.ncode.plugin.config.loot.LootSpawnPoint;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static ar.ncode.plugin.commands.spawn.ShowSpawnPoints.getWorldFromCommandContext;
import static ar.ncode.plugin.system.GraveSystem.setBlockWithRotation;

public class LootShowSpawnPointsCommand extends AbstractAsyncCommand {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public LootShowSpawnPointsCommand() {
		super("show", "Show all spawn points in the world with statues.");
	}

	public static boolean addBlockToWorldOnSpawnPoint(@NonNullDecl CommandContext commandContext, SpawnPoint spawnPoint,
	                                                  World world, String blockId) {
		long chunkIndex = ChunkUtil.indexChunkFromBlock(spawnPoint.getPosition().x, spawnPoint.getPosition().z);
		WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);

		Vector3i position = spawnPoint.getPosition().toVector3i();
		var rotationIndex = world.getBlockRotationIndex(
				(int) spawnPoint.getRotation().x,
				(int) spawnPoint.getRotation().y,
				(int) spawnPoint.getRotation().z
		);

		if (chunk == null) {
			try {

				return world.getChunkAsync(chunkIndex).thenApply(worldChunk -> {
					if (worldChunk == null) {
						return false;
					}


					return setBlockWithRotation(worldChunk, position.x, position.y, position.z, blockId, rotationIndex);
				}).get();
			} catch (Exception e) {
				LOGGER.atSevere().log("Could not load chunk for spawn point at position: {}, exception: {}", position, e);
				return false;
			}
		}

		return setBlockWithRotation(chunk, position.x, position.y, position.z, blockId, rotationIndex);
	}

	@NonNullDecl
	@Override
	protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
		return CompletableFuture.runAsync(() -> {
			World world = getWorldFromCommandContext(commandContext);
			if (world == null) return;

			world.execute(() -> {

				var instanceConfig = WorldAccessors.getWorldInstanceConfig(world);

				for (LootSpawnPoint lootSpawnPoint : instanceConfig.getLootSpawnPoints()) {
					SpawnPoint spawnPoint = lootSpawnPoint.getSpawnPoint();
					boolean result = addBlockToWorldOnSpawnPoint(commandContext, spawnPoint, world,
							"Furniture_Human_Ruins_Brazier");

					if (!result) {
						commandContext.sendMessage(Message.raw("Could not set flower at spawn point: " + spawnPoint.getPosition().toVector3i()));
					}
				}

				commandContext.sendMessage(Message.raw("All spawn points have been shown with statues."));

			});

		});

	}
}
