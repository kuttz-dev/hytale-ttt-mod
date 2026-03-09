package ar.ncode.plugin.ecs.system.player;

import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.SpawnPoint;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.RespawnSystems;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerRespawnSystem extends RespawnSystems.OnRespawnSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void teleportPlayerToRandomSpawnPoint(@NonNullDecl Ref<EntityStore> reference,
                                                        @NonNullDecl Store<EntityStore> store, InstanceConfig instanceConfig, World world
    ) {
        SpawnPoint[] points = instanceConfig.getPlayerSpawnPoints();
        if (points == null || points.length == 0 || !reference.isValid()) {
            return;
        }

        SpawnPoint randomPoint = points[ThreadLocalRandom.current().nextInt(points.length)];
        Teleport teleport = Teleport.createForPlayer(
                world,                          // World reference (required!)
                randomPoint.getPosition().clone(),          // Target position
                randomPoint.getRotation().clone()           // Target rotation (pitch, yaw, roll)
        );

        store.removeComponentIfExists(reference, Teleport.getComponentType());
        store.putComponent(reference, Teleport.getComponentType(), teleport);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), Player.getComponentType());
    }

    @Override
    public void onComponentRemoved(@NonNullDecl Ref<EntityStore> reference, @NonNullDecl DeathComponent deathComponent, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

    }
}
