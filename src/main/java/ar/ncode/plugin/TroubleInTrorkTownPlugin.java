package ar.ncode.plugin;

import ar.ncode.plugin.asset.WorldPreviewLoader;
import ar.ncode.plugin.commands.ChangeWorldCommand;
import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.commands.TttCommand;
import ar.ncode.plugin.commands.traitor.TraitorChatCommand;
import ar.ncode.plugin.component.GraveStoneWithNameplate;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.config.CustomConfig;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.config.instance.loot.LootTables;
import ar.ncode.plugin.interaction.ShowDeadPlayerInteraction;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.WorldPreview;
import ar.ncode.plugin.packet.filter.GuiPacketsFilter;
import ar.ncode.plugin.system.event.FinishCurrentMapEvent;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import ar.ncode.plugin.system.event.handler.FinishCurrentMapEventHandler;
import ar.ncode.plugin.system.event.handler.FinishCurrentRoundEventHandler;
import ar.ncode.plugin.system.event.handler.StartNewRoundEventHandler;
import ar.ncode.plugin.system.event.listener.DeadChatListener;
import ar.ncode.plugin.system.event.listener.*;
import ar.ncode.plugin.system.scheduled.DoubleTapDetector;
import ar.ncode.plugin.system.scheduled.PlayerHudUpdateSystem;
import ar.ncode.plugin.system.scheduled.WorldRoundTimeSystem;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class TroubleInTrorkTownPlugin extends JavaPlugin {

	public static final Path UNIVERSE_TEMPLATES_PATH = Paths.get("universe/templates");
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	public static TroubleInTrorkTownPlugin instance;
	public static Map<UUID, GameModeState> gameModeStateForWorld = new HashMap<>();
	public static Config<CustomConfig> config;
	public static List<WorldPreview> worldPreviews;
	public static String currentInstance;
	public static Map<String, Config<InstanceConfig>> instanceConfig = new HashMap<>();
	public static Config<LootTables> lootTables;
	/**
	 * Flag to track if a world transition (fade) is currently in progress.
	 * This prevents the "Cannot start a fade out while a fade completion callback is pending" error
	 * when multiple players trigger transitions simultaneously.
	 */
	public static volatile boolean isWorldTransitionInProgress = false;

	/**
	 * Thread-safe set of player UUIDs who are spectators (dead).
	 * Used by DeadChatListener to filter chat without accessing world thread.
	 */
	public static final java.util.Set<UUID> spectatorPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
	@SuppressWarnings("rawtypes")
	private List<EventRegistration> events = new ArrayList<>();
	private List<CommandRegistration> commands = new ArrayList<>();
	private List<PacketFilter> inboundPacketFilters = new ArrayList<>();

	public TroubleInTrorkTownPlugin(@Nonnull JavaPluginInit init) throws Exception {
		super(init);
		instance = this;
		config = this.withConfig("config", CustomConfig.CODEC);
		lootTables = this.withConfig("loot_tables", LootTables.CODEC);

		Path templates = Paths.get("universe/templates");

		try (Stream<Path> worlds = Files.list(templates)) {
			for (Path world : (Iterable<Path>) worlds::iterator) {
				instanceConfig.put(
						world.getFileName().toString(),
						this.withConfig(world.getFileName() + "_config", InstanceConfig.CODEC)
				);
			}
		} catch (Exception ignored) {
			LOGGER.atSevere().log("Failed to instances configs - {}", ignored);
		}
		LOGGER.atInfo().log("Starting plugin: " + this.getName() + " - version " + this.getManifest().getVersion().toString());
	}

	@SneakyThrows
	@Override
	protected void setup() {
		prepareConfigs();

		worldPreviews = WorldPreviewLoader.load(UNIVERSE_TEMPLATES_PATH, getDataDirectory());

		PlayerGameModeInfo.componentType = getEntityStoreRegistry().registerComponent(PlayerGameModeInfo.class, "PlayerGameModeInfo", PlayerGameModeInfo.CODEC);
		GraveStoneWithNameplate.componentType = getChunkStoreRegistry().registerComponent(GraveStoneWithNameplate.class,
				"GraveStoneWithNameplate", GraveStoneWithNameplate.CODEC);
		ConfirmedDeath.componentType = getEntityStoreRegistry().registerComponent(ConfirmedDeath.class, "ConfirmedDeath",
				ConfirmedDeath.CODEC);
		LostInCombat.componentType = getEntityStoreRegistry().registerComponent(LostInCombat.class, "LostInCombat",
				LostInCombat.CODEC);

		events.add(getEventRegistry().registerGlobal(PlayerReadyEvent.class, new PlayerReadyEventListener()));
		events.add(getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, new PlayerDisconnectEventListener()));
		events.add(getEventRegistry().registerGlobal(PlayerChatEvent.class, new DeadChatListener()));
		events.add(getEventRegistry().registerGlobal(StartNewRoundEvent.class, new StartNewRoundEventHandler()));
		events.add(getEventRegistry().registerGlobal(FinishCurrentRoundEvent.class, new FinishCurrentRoundEventHandler()));
		events.add(getEventRegistry().registerGlobal(FinishCurrentMapEvent.class, new FinishCurrentMapEventHandler()));

		commands.add(getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString())));
		commands.add(getCommandRegistry().registerCommand(new SpectatorMode()));
		commands.add(getCommandRegistry().registerCommand(new ChangeWorldCommand()));
		commands.add(getCommandRegistry().registerCommand(new TttCommand()));
		commands.add(getCommandRegistry().registerCommand(new TraitorChatCommand()));

		getCodecRegistry(Interaction.CODEC)
				.register("show_dead_player_info", ShowDeadPlayerInteraction.class, ShowDeadPlayerInteraction.CODEC);

		HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
			try {
				DoubleTapDetector.getInstance().tick();

			} catch (Exception e) {
				LOGGER.atWarning().log("Error in double-tap detector: " + e.getMessage());
			}

		}, 100L, 50L, TimeUnit.MILLISECONDS);

		LOGGER.atInfo().log("Plugin " + this.getName() + " setup completed!");
	}

	private void prepareConfigs() {
		Path configFilePath = Paths.get(getDataDirectory().toString(), "/config.json");
		if (!Files.exists(getDataDirectory()) || !Files.exists(configFilePath)) {
			config.save().thenRun(() -> LOGGER.atInfo().log("Saved default config"));
		}
		config.load().thenRun(() -> LOGGER.atInfo().log("Config loaded."));

		if (!Files.exists(Paths.get(getDataDirectory().toString(), "/loot_tables.json"))) {
			lootTables.save().thenRun(() -> LOGGER.atInfo().log("Saved default config"));
		}
		lootTables.load().thenRun(() -> LOGGER.atInfo().log("Config loaded."));

		instanceConfig.forEach((world, instanceCfg) -> {
			String instanceConfigFile = "/" + world + "_config.json";
			Path worldConfigPath = Paths.get(getDataDirectory().toString(), instanceConfigFile);

			Path instanceConfigPath = UNIVERSE_TEMPLATES_PATH.resolve(world);
			instanceConfigPath = instanceConfigPath.resolve("config.json");

			if (Files.exists(instanceConfigPath)) {
				try {
					Files.copy(instanceConfigPath, worldConfigPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				LOGGER.atInfo().log("Copied default instance config for {} from template.", world);
			}

			if (!Files.exists(worldConfigPath)) {
				instanceCfg.save().thenRun(() -> LOGGER.atInfo().log("Saved instance config for {}", world));
			}

			instanceCfg.load().thenRun(() -> LOGGER.atInfo().log("Instance config loaded for {}", world));
		});
	}

	@Override
	protected void start() {
		getEntityStoreRegistry().registerSystem(new SpectatorModeDamageListener());
		getEntityStoreRegistry().registerSystem(new PlayerDeathListener());
		getEntityStoreRegistry().registerSystem(new PlayerRespawnListener());
		getEntityStoreRegistry().registerSystem(new PlayerHudUpdateSystem());
		getEntityStoreRegistry().registerSystem(new WorldRoundTimeSystem());
		getEntityStoreRegistry().registerSystem(new BreakBlockListener());
		getEntityStoreRegistry().registerSystem(new PlaceBlockListener());

		inboundPacketFilters.add(PacketAdapters.registerInbound(new GuiPacketsFilter()));

		Universe.get().getWorlds().forEach((s, world) -> {
			gameModeStateForWorld.put(world.getWorldConfig().getUuid(), new GameModeState());
			world.execute(() -> {
				world.getWorldConfig().setCanSaveChunks(false);
				world.getWorldConfig().setGameTimePaused(true);
				world.getWorldConfig().setSpawningNPC(false);
			});
//			world.getDeathConfig().getRespawnController().respawnPlayer()
		});

		LOGGER.atInfo().log("Plugin started!");
	}

	@Override
	protected void shutdown() {
		inboundPacketFilters.forEach(PacketAdapters::deregisterInbound);
		commands.forEach(Registration::unregister);
		events.forEach(Registration::unregister);
		LOGGER.atInfo().log("Plugin shutting down!");
	}

}