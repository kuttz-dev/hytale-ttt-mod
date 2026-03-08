package ar.ncode.plugin.patches.server.core.universe.world.spawn;

import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.SpawnPoint;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class CustomSpawnProvider implements ISpawnProvider {

    @Nonnull
    public static BuilderCodec<GlobalSpawnProvider> CODEC = BuilderCodec.builder(GlobalSpawnProvider.class, GlobalSpawnProvider::new)
            .documentation("A spawn provider that provides a multiple preconfigured spawn points for all players.")
            .build();

    private final InstanceConfig instanceConfig;

    @Override
    public Transform getSpawnPoint(@NonNullDecl World world, @NonNullDecl UUID playerUUID) {
        SpawnPoint[] points = instanceConfig.getPlayerSpawnPoints();
        SpawnPoint randomPoint = points[ThreadLocalRandom.current().nextInt(points.length)];

        return new Transform(randomPoint.getPosition().clone(), randomPoint.getRotation().clone());
    }

    @Override
    public Transform[] getSpawnPoints() {
        return Arrays.stream(this.instanceConfig.getPlayerSpawnPoints())
                .map(spawn -> new Transform(spawn.getPosition().clone(), spawn.getRotation().clone()))
                .toList()
                .toArray(new Transform[0]);
    }

    @Override
    public boolean isWithinSpawnDistance(@NonNullDecl Vector3d position, double distance) {
        double distanceSquared = distance * distance;

        for (SpawnPoint point : this.instanceConfig.getPlayerSpawnPoints()) {
            if (position.distanceSquaredTo(point.getPosition()) < distanceSquared) {
                return true;
            }
        }

        return false;
    }
}
