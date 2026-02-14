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
import ar.ncode.plugin.config.WeaponsConfig;
import ar.ncode.plugin.config.instance.InstanceConfig;
import ar.ncode.plugin.interaction.PickUpWeaponInteraction;
import ar.ncode.plugin.interaction.ShowDeadPlayerInteraction;
import ar.ncode.plugin.interaction.TestPlayerRole;
import ar.ncode.plugin.interaction.TestPlayerRolePotion;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.WorldPreview;
import ar.ncode.plugin.packet.filter.GuiPacketsFilter;
import ar.ncode.plugin.system.ItemPickUpSystem;
import ar.ncode.plugin.system.event.FinishCurrentMapEvent;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import ar.ncode.plugin.system.event.handler.FinishCurrentMapEventHandler;
import ar.ncode.plugin.system.event.handler.FinishCurrentRoundEventHandler;
import ar.ncode.plugin.system.event.handler.StartNewRoundEventHandler;
import ar.ncode.plugin.system.event.listener.ChatListener;
import ar.ncode.plugin.system.event.listener.InteractiveItemPickUpListener;
import ar.ncode.plugin.system.event.listener.SpectatorModeDamageListener;
import ar.ncode.plugin.system.event.listener.block.BreakBlockListener;
import ar.ncode.plugin.system.event.listener.block.DamageBlockListener;
import ar.ncode.plugin.system.event.listener.block.PlaceBlockListener;
import ar.ncode.plugin.system.event.listener.player.PlayerDisconnectEventListener;
import ar.ncode.plugin.system.event.listener.player.PlayerReadyEventListener;
import ar.ncode.plugin.system.player.PlayerDeathSystem;
import ar.ncode.plugin.system.player.PlayerRespawnSystem;
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
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static ar.ncode.plugin.model.CustomPermissions.ADMIN_PERMISSIONS;
import static ar.ncode.plugin.model.CustomPermissions.TTT_ADMIN_GROUP;
import static ar.ncode.plugin.model.CustomPermissions.TTT_USER_GROUP;
import static ar.ncode.plugin.model.CustomPermissions.USER_PERMISSIONS;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class TroubleInTrorkTownPlugin extends JavaPlugin {

	public final Path templatesPath = getDataDirectory().resolve("maps");
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
	public static List<WorldPreview> worldPreviews;
	public static UUID currentInstance;
	public static Map<String, Config<InstanceConfig>> instanceConfig = new HashMap<>();
	/**
	 * Flag to track if a world transition (fade) is currently in progress.
	 * This prevents the "Cannot start a fade out while a fade completion callback is pending" error
	 * when multiple players trigger transitions simultaneously.
	 */
	public static volatile boolean isWorldTransitionInProgress = false;
	@SuppressWarnings("rawtypes")
	private List<EventRegistration> events = new ArrayList<>();
	private List<CommandRegistration> commands = new ArrayList<>();
	private List<PacketFilter> inboundPacketFilters = new ArrayList<>();

	public TroubleInTrorkTownPlugin(@Nonnull JavaPluginInit init) throws Exception {
		super(init);
		instance = this;
		config = this.withConfig("config", CustomConfig.CODEC);
		weaponsConfig = this.withConfig("weapons_config", WeaponsConfig.CODEC);

		try (Stream<Path> worlds = Files.list(templatesPath)) {
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

		worldPreviews = WorldPreviewLoader.load(templatesPath, getDataDirectory());

		PlayerGameModeInfo.componentType = getEntityStoreRegistry().registerComponent(PlayerGameModeInfo.class, "PlayerGameModeInfo", PlayerGameModeInfo.CODEC);
		GraveStoneWithNameplate.componentType = getChunkStoreRegistry().registerComponent(GraveStoneWithNameplate.class,
				"GraveStoneWithNameplate", GraveStoneWithNameplate.CODEC);
		ConfirmedDeath.componentType = getEntityStoreRegistry().registerComponent(ConfirmedDeath.class, "ConfirmedDeath",
				ConfirmedDeath.CODEC);
		LostInCombat.componentType = getEntityStoreRegistry().registerComponent(LostInCombat.class, "LostInCombat",
				LostInCombat.CODEC);

		events.add(getEventRegistry().registerGlobal(PlayerReadyEvent.class, new PlayerReadyEventListener()));
		events.add(getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, new PlayerDisconnectEventListener()));
		events.add(getEventRegistry().registerGlobal(PlayerChatEvent.class, new ChatListener()));
		events.add(getEventRegistry().registerGlobal(StartNewRoundEvent.class, new StartNewRoundEventHandler()));
		events.add(getEventRegistry().registerGlobal(FinishCurrentRoundEvent.class, new FinishCurrentRoundEventHandler()));
		events.add(getEventRegistry().registerGlobal(FinishCurrentMapEvent.class, new FinishCurrentMapEventHandler()));

		commands.add(getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString())));
		commands.add(getCommandRegistry().registerCommand(new SpectatorMode()));
		commands.add(getCommandRegistry().registerCommand(new ChangeWorldCommand()));
		commands.add(getCommandRegistry().registerCommand(new TttCommand()));
		commands.add(getCommandRegistry().registerCommand(new TraitorChatCommand()));

		getCodecRegistry(Interaction.CODEC)
				.register("test_player_role", TestPlayerRole.class, TestPlayerRole.CODEC)
				.register("test_player_role_potion", TestPlayerRolePotion.class, TestPlayerRolePotion.CODEC)
				.register("show_dead_player_info", ShowDeadPlayerInteraction.class, ShowDeadPlayerInteraction.CODEC)
				.register("pickup_weapon_interaction", PickUpWeaponInteraction.class, PickUpWeaponInteraction.CODEC);

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
		if (!Files.exists(getDataDirectory()) || !Files.exists(getConfigFilePath())) {
			config.save().thenRun(() -> LOGGER.atInfo().log("Saved default config"));
		}
		config.load().thenRun(() -> LOGGER.atInfo().log("Gamemode config loaded."));

		instanceConfig.forEach((world, instanceCfg) -> {
			String instanceConfigFile = "/" + world + "_config.json";
			Path worldConfigPath = Paths.get(getDataDirectory().toString(), instanceConfigFile);

			Path instanceConfigPath = templatesPath.resolve(world);
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

		if (!Files.exists(getWeaponsConfigFilePath())) {
			weaponsConfig.save().thenRun(() -> LOGGER.atInfo().log("Saved default config"));
		}
		weaponsConfig.load().thenRun(() -> LOGGER.atInfo().log("Config loaded."));
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

}
