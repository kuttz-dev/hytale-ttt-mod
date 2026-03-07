package ar.ncode.plugin.patches.advice;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Field;
import java.util.UUID;

public class ExplosionUtilsAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(2) ExplosionConfig config,
            @Advice.Argument(4) CommandBuffer<EntityStore> commandBuffer
    ) {
        World world = commandBuffer.getExternalData().getWorld();
        UUID worldUUID = world.getWorldConfig().getUuid();
        var instanceConfig = TroubleInTrorkTownPlugin.instanceConfigs.get(worldUUID);
        if (config == null || instanceConfig == null) {
            return;
        }

        try {
            if (!instanceConfig.get().isMapDestructibleByExplosions()) {
                Field damageBlocks = config.getClass().getDeclaredField("damageBlocks");
                damageBlocks.setAccessible(true);
                damageBlocks.setBoolean(config, false);
            }

        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // The field does not exist in this version, skip patching.
        }
    }

}



