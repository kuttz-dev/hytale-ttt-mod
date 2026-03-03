package ar.ncode.plugin.patches;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.accessors.WorldAccessors;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class ExplosionUtilsAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(2) ExplosionConfig config,
            @Advice.Argument(4) CommandBuffer<EntityStore> commandBuffer
    ) {
        String worldName = WorldAccessors.getWorldNameForInstance(commandBuffer.getExternalData().getWorld());
        var instanceConfig = TroubleInTrorkTownPlugin.instanceConfig.get(worldName);
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



