package ar.ncode.plugin.patches;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public final class ExplosionMethodTemplate {

    private ExplosionMethodTemplate() {
    }

    public static void ttt$onExplosionEnter(ExplosionConfig config, CommandBuffer<EntityStore> commandBuffer) {
        if (config == null || commandBuffer == null) {
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        if (world == null || world.getWorldConfig() == null) {
            return;
        }

        try {
            UUID worldUUID = world.getWorldConfig().getUuid();
            if (worldUUID == null) {
                return;
            }

            PluginManager pluginManager = PluginManager.get();
            if (pluginManager == null) {
                return;
            }

            PluginBase plugin = pluginManager.getPlugin(new PluginIdentifier("ncode", "ttt"));
            if (plugin == null) {
                return;
            }

            ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();
            Class<?> pluginClass = Class.forName("ar.ncode.plugin.TroubleInTrorkTownPlugin", false, pluginClassLoader);
            Field instanceConfigsField = pluginClass.getField("instanceConfigs");
            Object instanceConfigs = instanceConfigsField.get(null);

            if (!(instanceConfigs instanceof Map<?, ?> configsByWorld)) {
                return;
            }

            Object configHandle = configsByWorld.get(worldUUID);
            if (configHandle == null) {
                return;
            }

            Method getConfig = configHandle.getClass().getMethod("get");
            Object instanceConfig = getConfig.invoke(configHandle);
            if (instanceConfig == null) {
                return;
            }

            Method isDestructible = instanceConfig.getClass().getMethod("isMapDestructibleByExplosions");
            Object result = isDestructible.invoke(instanceConfig);
            if (!(result instanceof Boolean value) || value) {
                return;
            }

            Field damageBlocks = ExplosionConfig.class.getDeclaredField("damageBlocks");
            damageBlocks.setAccessible(true);
            damageBlocks.setBoolean(config, false);

        } catch (ReflectiveOperationException ignored) {
            // Class layout changed or plugin state unavailable; skip the patch.
        }
    }
}
