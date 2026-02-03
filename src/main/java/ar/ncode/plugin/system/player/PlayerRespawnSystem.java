package ar.ncode.plugin.system.player;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.SpawnPoint;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.RespawnSystems;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

import static ar.ncode.plugin.accessors.WorldAccessors.getWorldNameForInstance;

public class PlayerRespawnSystem extends RespawnSystems.OnRespawnSystem {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public static void teleportPlayerToRandomSpawnPoint(@NonNullDecl Ref<EntityStore> reference,
	                                                    @NonNullDecl Store<EntityStore> store, InstanceConfig instanceConfig, World world) {
		SpawnPoint[] points = instanceConfig.getPlayerSpawnPoints();
		if (points == null || points.length == 0) {
			return;
		}

		SpawnPoint randomPoint = points[ThreadLocalRandom.current().nextInt(points.length)];

		TransformComponent transform = store.getComponent(reference, TransformComponent.getComponentType());

		if (transform == null) {
			return;
		}

		Teleport teleport = Teleport.createForPlayer(
				world,                          // World reference (required!)
				randomPoint.getPosition().clone(),          // Target position
				randomPoint.getRotation().clone()           // Target rotation (pitch, yaw, roll)
		);

		store.addComponent(reference, Teleport.getComponentType(), teleport);
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return Query.and(PlayerRef.getComponentType(), Player.getComponentType());
	}

	@Override
	public void onComponentRemoved(@NonNullDecl Ref<EntityStore> reference, @NonNullDecl DeathComponent deathComponent, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
		Player player = store.getComponent(reference, Player.getComponentType());

		if (player == null) {
			return;
		}

		World world = player.getWorld();
		if (world == null || world.getWorldConfig().getDisplayName() == null) {
			return;
		}

		String worldName = getWorldNameForInstance(world);
		InstanceConfig instanceConfig = TroubleInTrorkTownPlugin.instanceConfig.get(worldName).get();

		world.execute(() -> teleportPlayerToRandomSpawnPoint(reference, store, instanceConfig, world));
	}
}
