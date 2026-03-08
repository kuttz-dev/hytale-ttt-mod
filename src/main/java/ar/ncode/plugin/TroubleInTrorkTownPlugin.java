package ar.ncode.plugin;

import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.asset.WorldPreviewLoader;
import ar.ncode.plugin.config.CustomConfig;
import ar.ncode.plugin.config.WeaponsConfig;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.InstanceConfigWrapper;
import ar.ncode.plugin.ecs.commands.ChangeWorldCommand;
import ar.ncode.plugin.ecs.commands.SpectatorMode;
import ar.ncode.plugin.ecs.commands.TttCommand;
import ar.ncode.plugin.ecs.commands.traitor.TraitorChatCommand;
import ar.ncode.plugin.ecs.component.DeadPlayerGravestoneComponent;
import ar.ncode.plugin.ecs.component.DeadPlayerInfoComponent;
import ar.ncode.plugin.ecs.component.PlayerGameModeInfo;
import ar.ncode.plugin.ecs.component.death.ConfirmedDeath;
import ar.ncode.plugin.ecs.component.death.LostInCombat;
import ar.ncode.plugin.ecs.interaction.PickUpWeaponInteraction;
import ar.ncode.plugin.ecs.interaction.ShowDeadPlayerInfoInteraction;
import ar.ncode.plugin.ecs.interaction.TestPlayerRole;
import ar.ncode.plugin.ecs.interaction.TestPlayerRolePotion;
import ar.ncode.plugin.ecs.npc.ShowDeadPlayerInfoAction;
import ar.ncode.plugin.ecs.system.ItemPickUpSystem;
import ar.ncode.plugin.ecs.system.event.FinishCurrentMapEvent;
import ar.ncode.plugin.ecs.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.ecs.system.event.StartNewRoundEvent;
import ar.ncode.plugin.ecs.system.event.handler.FinishCurrentMapEventHandler;
import ar.ncode.plugin.ecs.system.event.handler.FinishCurrentRoundEventHandler;
import ar.ncode.plugin.ecs.system.event.handler.StartNewRoundEventHandler;
import ar.ncode.plugin.ecs.system.event.listener.ChatListener;
import ar.ncode.plugin.ecs.system.event.listener.InteractiveItemPickUpListener;
import ar.ncode.plugin.ecs.system.event.listener.RemoveWorldListener;
import ar.ncode.plugin.ecs.system.event.listener.SpectatorModeDamageListener;
import ar.ncode.plugin.ecs.system.event.listener.block.BreakBlockListener;
import ar.ncode.plugin.ecs.system.event.listener.block.DamageBlockListener;
import ar.ncode.plugin.ecs.system.event.listener.block.PlaceBlockListener;
import ar.ncode.plugin.ecs.system.player.PlayerDeathSystem;
import ar.ncode.plugin.ecs.system.player.PlayerRespawnSystem;
import ar.ncode.plugin.ecs.system.player.event.listener.PlayerConnectEventListener;
import ar.ncode.plugin.ecs.system.player.event.listener.PlayerDisconnectEventListener;
import ar.ncode.plugin.ecs.system.player.event.listener.PlayerReadyEventListener;
import ar.ncode.plugin.ecs.system.scheduled.DoubleTapDetector;
import ar.ncode.plugin.ecs.system.scheduled.PlayerHudUpdateSystem;
import ar.ncode.plugin.ecs.system.scheduled.WorldRoundTimeSystem;
import ar.ncode.plugin.exception.ConfigError;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.packet.filter.PacketsFilter;
import ar.ncode.plugin.patches.CancelExplosionInteraction;
import ar.ncode.plugin.patches.server.core.universe.world.spawn.CustomSpawnProvider;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginState;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.NPCPlugin;
import lombok.SneakyThrows;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bstats.hytale.Metrics;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static ar.ncode.plugin.model.CustomPermissions.*;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class TroubleInTrorkTownPlugin extends JavaPlugin {

    public static final Set<UUID> spectatorPlayers = ConcurrentHashMap.newKeySet();
    /**
     * Thread-safe set of component UUIDs who are spectators (dead).
     * Used by DeadChatListener to filter chat without accessing world thread.
     */
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static TroubleInTrorkTownPlugin instance;
    public static Map<UUID, GameModeState> gameModeStateForWorld = new HashMap<>();
    public static Config<CustomConfig> config;
    public static Config<WeaponsConfig> weaponsConfig;
    public static UUID currentInstance;
    public static Map<String, InstanceConfigWrapper> mapTemplateConfig = new HashMap<>();
    public static Map<UUID, Config<InstanceConfig>> instanceConfigs = new HashMap<>();
    public final Path templatesPath = getDataDirectory().resolve("maps");
    @SuppressWarnings("rawtypes")
    private List<EventRegistration> events = new ArrayList<>();
    private List<CommandRegistration> commands = new ArrayList<>();
    private List<PacketFilter> inboundPacketFilters = new ArrayList<>();
    private Metrics metrics;

    public TroubleInTrorkTownPlugin(@Nonnull JavaPluginInit init) throws Exception {
        super(init);
        instance = this;
        config = this.withConfig("config", CustomConfig.CODEC);
        weaponsConfig = this.withConfig("weapons_config", WeaponsConfig.CODEC);

        gatherMapsConfig();

        ByteBuddyAgent.install();
        LOGGER.atInfo().log("Starting plugin: " + this.getName() + " - version " + this.getManifest().getVersion().toString());
    }

    public void gatherMapsConfig() throws IOException {
        if (!Files.exists(templatesPath)) {
            Files.createDirectories(templatesPath);
        }

        List<CompletableFuture<Void>> maps = new ArrayList<>();
        try (Stream<Path> worlds = Files.list(templatesPath)) {
            for (Path mapFolder : (Iterable<Path>) worlds::iterator) {
                if (!Files.exists(mapFolder.resolve("config.json"))) {
                    LOGGER.atWarning().log("Map %s does not have a config.json file", mapFolder);
                    continue;
                }

                if (!Files.exists(mapFolder.resolve("preview.png"))) {
                    LOGGER.atWarning().log("Map %s does not have a preview.png file", mapFolder);
                    continue;
                }

                if (!Files.exists(mapFolder.resolve("instance.bson"))) {
                    LOGGER.atWarning().log("Map %s does not have a instance.bson file", mapFolder);
                    continue;
                }

                maps.add(BsonUtil.readDocument(mapFolder.resolve("instance.bson"))
                        .thenAcceptAsync(file -> {
                            var mapField = file.get("DisplayName");
                            if (mapField == null || !mapField.isString()) {
                                LOGGER.atSevere().log("Failed to read map name from instance.bson for map template %s - skipping config load", mapFolder.getFileName());
                                return;
                            }

                            String displayName = mapField.asString().getValue();
                            String mapSafeName = WorldAccessors.getSafeWorldName(displayName);

                            if (mapSafeName == null) {
                                LOGGER.atSevere().log("Failed to read map name from instance.bson for map template %s - skipping config load", mapFolder.getFileName());
                                return;
                            }

                            Config<InstanceConfig> config = this.withConfig(mapFolder, "config", InstanceConfig.CODEC);
                            InstanceConfigWrapper configWrapper = new InstanceConfigWrapper(
                                    config, mapFolder, mapSafeName, displayName, mapFolder.getFileName().toString()
                            );

                            mapTemplateConfig.put(
                                    mapSafeName,
                                    configWrapper
                            );
                        })
                );
            }

        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to load instance configs - %s", e.getMessage());
        }

        CompletableFuture<?>[] promisesArray = maps.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(promisesArray).thenRun(() ->
                LOGGER.atInfo().log("Finished loading map templates. Total maps found: %d", mapTemplateConfig.size())
        );
    }

    @SneakyThrows
    @Override
    protected void setup() {
        loadConfigs();

        WorldPreviewLoader.loadInstancesAsAssets(templatesPath, getDataDirectory());

        PlayerGameModeInfo.componentType = getEntityStoreRegistry().registerComponent(PlayerGameModeInfo.class, "PlayerGameModeInfo", PlayerGameModeInfo.CODEC);

        DeadPlayerInfoComponent.componentType = getEntityStoreRegistry().registerComponent(
                DeadPlayerInfoComponent.class, "GraveStoneWithNameplate", DeadPlayerInfoComponent.CODEC
        );
        DeadPlayerGravestoneComponent.componentType = getChunkStoreRegistry().registerComponent(
                DeadPlayerGravestoneComponent.class, "DeadPlayerGravestoneComponent", DeadPlayerGravestoneComponent.CODEC
        );

        ConfirmedDeath.componentType = getEntityStoreRegistry().registerComponent(
                ConfirmedDeath.class, "ConfirmedDeath", ConfirmedDeath.CODEC
        );
        LostInCombat.componentType = getEntityStoreRegistry().registerComponent(
                LostInCombat.class, "LostInCombat", LostInCombat.CODEC
        );
        ISpawnProvider.CODEC.register("CustomSpawnPoints", CustomSpawnProvider.class, CustomSpawnProvider.CODEC);

        events.add(getEventRegistry().registerGlobal(PlayerReadyEvent.class, new PlayerReadyEventListener()));
        events.add(getEventRegistry().registerGlobal(PlayerConnectEvent.class, new PlayerConnectEventListener()));
        events.add(getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, new PlayerDisconnectEventListener()));
        events.add(getEventRegistry().registerAsyncGlobal(PlayerChatEvent.class, future ->
                        future.thenCompose(e -> {
                            // If the event was already cancelled by another plugin, skip processing to avoid unnecessary overhead
                            if (e.isCancelled()) {
                                return CompletableFuture.completedFuture(e);
                            }

                            return CompletableFuture.supplyAsync(() -> new ChatListener().accept(e));
                        })
                )
        );

        events.add(getEventRegistry().registerGlobal(StartNewRoundEvent.class, new StartNewRoundEventHandler()));
        events.add(getEventRegistry().registerGlobal(FinishCurrentRoundEvent.class, new FinishCurrentRoundEventHandler()));
        events.add(getEventRegistry().registerGlobal(FinishCurrentMapEvent.class, new FinishCurrentMapEventHandler()));
        events.add(getEventRegistry().registerGlobal(RemoveWorldEvent.class, new RemoveWorldListener()));

        commands.add(getCommandRegistry().registerCommand(new SpectatorMode()));
        commands.add(getCommandRegistry().registerCommand(new ChangeWorldCommand()));
        commands.add(getCommandRegistry().registerCommand(new TttCommand()));
        commands.add(getCommandRegistry().registerCommand(new TraitorChatCommand()));

        getCodecRegistry(Interaction.CODEC)
                .register("test_player_role", TestPlayerRole.class, TestPlayerRole.CODEC)
                .register("test_player_role_potion", TestPlayerRolePotion.class, TestPlayerRolePotion.CODEC)
                .register("show_dead_player_info", ShowDeadPlayerInfoInteraction.class, ShowDeadPlayerInfoInteraction.CODEC)
                .register("pickup_weapon_interaction", PickUpWeaponInteraction.class, PickUpWeaponInteraction.CODEC);


        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                DoubleTapDetector.getInstance().tick();

            } catch (Exception e) {
                LOGGER.atWarning().log("Error in double-tap detector: " + e.getMessage());
            }

        }, 100L, 50L, TimeUnit.MILLISECONDS);

        // Register NPC core action builder here so it's available when NPC JSON files are loaded
        if (NPCPlugin.get() != null) {
            NPCPlugin.get().registerCoreComponentType("ShowDeadPlayerInfoAction", ShowDeadPlayerInfoAction.Builder::new);
            LOGGER.atInfo().log("Registered NPC core component builder: ShowDeadPlayerInfoAction");
        } else {
            LOGGER.atSevere().log("NPCPlugin not available yet; ShowDeadPlayerInfoAction builder not registered in setup().");
        }

        // Apply patches
        new CancelExplosionInteraction().apply();

        // Metrics
        this.metrics = new Metrics(this, 29880);

        LOGGER.atInfo().log("Plugin " + this.getName() + " setup completed!");
    }

    private void loadConfigs() throws IOException {
        if (!Files.exists(getDataDirectory()) || !Files.exists(getConfigFilePath())) {
            config.save().thenRun(() -> LOGGER.atInfo().log("Saved default config"));
        }
        config.load().thenRun(() -> LOGGER.atInfo().log("Gamemode config loaded."));

        if (!Files.exists(getWeaponsConfigFilePath())) {
            weaponsConfig.save().thenRun(() -> LOGGER.atInfo().log("Saved default config"));
        }
        weaponsConfig.load().thenRun(() -> LOGGER.atInfo().log("Config loaded."));

        loadMapsConfig();
    }

    public void loadMapsConfig() {
        for (Map.Entry<String, InstanceConfigWrapper> entry : mapTemplateConfig.entrySet()) {
            String map = entry.getKey();
            InstanceConfigWrapper instanceCfgWrapper = entry.getValue();

            if (!Files.exists(instanceCfgWrapper.getPath().resolve("config.json"))) {
                instanceCfgWrapper.getInstanceConfig().save().thenRun(() -> LOGGER.atInfo().log("Saved default instance config for %s", map));
            }

            instanceCfgWrapper.getInstanceConfig().load().thenRun(() -> LOGGER.atInfo().log("Instance config loaded for %s", map));
        }
    }

    private Path getConfigFilePath() {
        return getDataDirectory().resolve("config.json");
    }

    private Path getWeaponsConfigFilePath() {
        return getDataDirectory().resolve("weapons_config.json");
    }

    @Override
    protected void start() {
        getEntityStoreRegistry().registerSystem(new SpectatorModeDamageListener());
        getEntityStoreRegistry().registerSystem(new PlayerDeathSystem());
        getEntityStoreRegistry().registerSystem(new PlayerRespawnSystem());
        getEntityStoreRegistry().registerSystem(new PlayerHudUpdateSystem());
        getEntityStoreRegistry().registerSystem(new WorldRoundTimeSystem());
        getEntityStoreRegistry().registerSystem(new BreakBlockListener());
        getEntityStoreRegistry().registerSystem(new DamageBlockListener());
        getEntityStoreRegistry().registerSystem(new PlaceBlockListener());
        getEntityStoreRegistry().registerSystem(new InteractiveItemPickUpListener());
        getEntityStoreRegistry().registerSystem(new ItemPickUpSystem());

        inboundPacketFilters.add(PacketAdapters.registerInbound(new PacketsFilter()));

        Universe.get().getWorlds().forEach((s, world) -> {
            gameModeStateForWorld.put(world.getWorldConfig().getUuid(), new GameModeState());
            world.execute(() -> {
                world.getWorldConfig().setCanSaveChunks(false);
                world.getWorldConfig().setGameTimePaused(true);
                world.getWorldConfig().setSpawningNPC(false);
            });
        });

        PermissionsModule permissions = PermissionsModule.get();
        permissions.addGroupPermission(TTT_USER_GROUP, USER_PERMISSIONS);
        permissions.addGroupPermission(TTT_ADMIN_GROUP, ADMIN_PERMISSIONS);

        LOGGER.atInfo().log("Plugin started!");
    }

    @Override
    protected void shutdown() {
        inboundPacketFilters.forEach(PacketAdapters::deregisterInbound);
        commands.forEach(Registration::unregister);
        events.forEach(Registration::unregister);
        LOGGER.atInfo().log("Plugin shutting down!");
    }

    @Nonnull
    protected <T> Config<T> withConfig(Path path, @Nonnull String name, @Nonnull BuilderCodec<T> configCodec) {
        if (this.getState() != PluginState.NONE) {
            throw new IllegalStateException("Must be called before setup");
        } else {
            Config<T> config = new Config<>(path, name, configCodec);

            try {
                Field field = PluginBase.class.getDeclaredField("configs");
                field.setAccessible(true);

                @SuppressWarnings("unchecked")
                var configs = (List<Config<?>>) field.get(this);

                if (configs == null) {
                    throw new ConfigError("Failed to obtain config list when registering config: " + name);
                }

                configs.add(config);

            } catch (Exception e) {
                throw new ConfigError("Failed to register config: " + name, e);
            }
            return config;
        }
    }
}
