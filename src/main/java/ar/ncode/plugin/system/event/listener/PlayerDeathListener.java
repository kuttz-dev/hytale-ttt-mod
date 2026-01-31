package ar.ncode.plugin.system.event.listener;

import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.GraveStoneWithNameplate;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.component.enums.PlayerRole;
import ar.ncode.plugin.component.enums.RoundState;
import ar.ncode.plugin.model.DamageCause;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.system.GraveSystem;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.GameModeState.timeFormatter;
import static ar.ncode.plugin.system.event.handler.FinishCurrentRoundEventHandler.roundShouldEnd;

public class PlayerDeathListener extends DeathSystems.OnDeathSystem {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public static void updatePlayerCounts(PlayerRole playerRole, GameModeState gameModeState) {
		if (PlayerRole.TRAITOR.equals(playerRole)) {
			gameModeState.traitorsAlive -= 1;

		} else if (PlayerRole.INNOCENT.equals(playerRole) || PlayerRole.DETECTIVE.equals(playerRole)) {
			gameModeState.innocentsAlive -= 1;
		}
	}

	private static int calculateKarmaForAttacker(PlayerRole attackerPlayerRole, PlayerRole playerRole) {
		int value = 0;
		if (PlayerRole.TRAITOR.equals(attackerPlayerRole)) {
			if (PlayerRole.INNOCENT.equals(playerRole)) {
				value = config.get().getKaramPointsForTraitorKillingInnocent();

			} else if (PlayerRole.TRAITOR.equals(playerRole)) {
				value = config.get().getKaramPointsForTraitorKillingTraitor();

			} else if (PlayerRole.DETECTIVE.equals(playerRole)) {
				value = config.get().getKaramPointsForTraitorKillingDetective();
			}

		} else if (PlayerRole.INNOCENT.equals(attackerPlayerRole)) {
			if (PlayerRole.TRAITOR.equals(playerRole)) {
				value = config.get().getKarmaPointsForInnocentKillingTraitor();

			} else if (PlayerRole.INNOCENT.equals(playerRole)) {
				value = config.get().getKarmaPointsForInnocentKillingInnocent();

			} else {
				value = config.get().getKarmaPointsForInnocentKillingDetective();

			}

		} else if (PlayerRole.DETECTIVE.equals(attackerPlayerRole)) {
			if (PlayerRole.TRAITOR.equals(playerRole)) {
				value = config.get().getKarmaPointsForDetectiveKillingTraitor();

			} else if (PlayerRole.INNOCENT.equals(playerRole)) {
				value = config.get().getKarmaPointsForDetectiveKillingInnocent();

			} else {
				value = config.get().getKarmaPointsForDetectiveKillingDetective();
			}

		}
		return value;
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return Query.and(PlayerRef.getComponentType(), Player.getComponentType());
	}

	@Override
	public void onComponentAdded(@NonNullDecl Ref<EntityStore> reference, @NonNullDecl DeathComponent deathComponent,
	                             @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer
	) {
		// Get reference to the damaged entity
		Player player = store.getComponent(reference, Player.getComponentType());

		if (player == null) {
			return;
		}

		// Close any open pages, including death screen
		deathComponent.setShowDeathMenu(false);
		PageManager pageManager = player.getPageManager();
		pageManager.setPage(reference, store, Page.None);

		PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());
		PlayerGameModeInfo playerInfo = store.getComponent(reference, PlayerGameModeInfo.componentType);

		World world = player.getWorld();
		if (world == null || playerInfo == null) {
			return;
		}

		GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());

		if (gameModeState == null) {
			return;
		}

		if (!RoundState.IN_GAME.equals(gameModeState.roundState)) {
			return;
		}

		PlayerRole playerRole = playerInfo.getRole();

		world.execute(() -> {
			store.ensureComponent(reference, LostInCombat.componentType);
			player.getInventory().clear();
			SpectatorMode.setGameModeToSpectator(playerRef, reference);
		});

		if (deathComponent.getDeathInfo() == null) {
			return;
		}

		Damage.Source source = deathComponent.getDeathInfo().getSource();

		if (source instanceof Damage.EntitySource attackerRef) {
			PlayerRef attackerPlayerRef = store.getComponent(attackerRef.getRef(), PlayerRef.getComponentType());
			PlayerGameModeInfo attackerPlayerInfo = store
					.getComponent(attackerRef.getRef(), PlayerGameModeInfo.componentType);

			if (attackerPlayerRef != null && attackerPlayerInfo != null) {
				PlayerRole attackerPlayerRole = attackerPlayerInfo.getRole();
				int value = calculateKarmaForAttacker(attackerPlayerRole, playerRole);
				gameModeState.karmaUpdates.merge(attackerPlayerRef.getUuid(), value, Integer::sum);
			}
		}

		GraveStoneWithNameplate graveStone = new GraveStoneWithNameplate();
		graveStone.setTimeOfDeath(gameModeState.getRoundRemainingTime().format(timeFormatter));
		graveStone.setDeadPlayerReference(reference);
		graveStone.setDeadPlayerRole(playerRole);
		graveStone.setDeadPlayerName(player.getDisplayName());

		if (deathComponent.getDeathCause() != null) {
			DamageCause damageCause = DamageCause.valueOf(deathComponent.getDeathCause().getId().toUpperCase());
			graveStone.setCauseOfDeath(damageCause);
		}

		playerInfo.setRole(PlayerRole.SPECTATOR);
		TroubleInTrorkTownPlugin.spectatorPlayers.add(playerRef.getUuid()); // Track for chat filtering
		playerInfo.getHud().update();

		updatePlayerCounts(playerRole, gameModeState);
		GraveSystem.spawnGraveAtPlayerDeath(world, graveStone, reference);

		if (roundShouldEnd(gameModeState)) {
			HytaleServer.get().getEventBus()
					.dispatchForAsync(FinishCurrentRoundEvent.class)
					.dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
		}
	}


}
