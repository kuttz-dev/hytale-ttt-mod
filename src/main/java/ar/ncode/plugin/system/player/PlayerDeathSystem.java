package ar.ncode.plugin.system.player;

import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.GraveStoneWithNameplate;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.model.DamageCause;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoleGroup;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.system.GraveSystem;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import com.hypixel.hytale.common.util.CompletableFutureUtil;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.Set;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.accessors.PlayerAccessors.getPlayerFrom;
import static ar.ncode.plugin.model.GameModeState.timeFormatter;
import static ar.ncode.plugin.system.event.handler.FinishCurrentRoundEventHandler.roundShouldEnd;

@Getter
public class PlayerDeathSystem extends DeathSystems.OnDeathSystem {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency<>(Order.BEFORE,
			DeathSystems.PlayerDeathScreen.class, OrderPriority.NORMAL));

	public static void updatePlayerCountsOnPlayerDeath(PlayerRef playerRef, CustomRole role, GameModeState gameModeState) {
		if (RoleGroup.TRAITOR.equals(role.getRoleGroup())) {
			gameModeState.traitorsAlive.remove(playerRef.getUuid());

		} else if (RoleGroup.INNOCENT.equals(role.getRoleGroup())) {
			gameModeState.innocentsAlice.remove(playerRef.getUuid());
		}

		gameModeState.spectators.add(playerRef.getUuid());
	}

	private static int calculateKarmaForAttacker(CustomRole attackerRole, CustomRole attackedRole) {
		int value;

		if (attackerRole.getRoleGroup().equals(attackedRole.getRoleGroup())) {
			value = config.get().getKaramPointsForKillingSameRoleGroup();
		} else {
			value = config.get().getKaramPointsForKillingOppositeRoleGroup();
		}

		if (!attackedRole.isSecretRole()) {
			value = 2 * value;
		}

		return value;
	}

	private static void updateAttackerKarma(@NonNullDecl DeathComponent deathComponent, PlayerComponents player, GameModeState gameModeState) {
		if (deathComponent.getDeathInfo() == null) {
			return;
		}

		Damage.Source source = deathComponent.getDeathInfo().getSource();

		if (source instanceof Damage.EntitySource attackerRef) {
			var attacker = getPlayerFrom(attackerRef.getRef()).orElse(null);
			if (attacker == null) return;

			if (attacker.refComponent() != null && attacker.info() != null) {
				int value = calculateKarmaForAttacker(attacker.info().getCurrentRoundRole(), player.info().getCurrentRoundRole());
				gameModeState.karmaUpdates.merge(attacker.refComponent().getUuid(), value, Integer::sum);
			}
		}
	}

	private static void spawnGraveStone(@NonNullDecl Ref<EntityStore> reference, @NonNullDecl DeathComponent deathComponent, GameModeState gameModeState, PlayerComponents player, World world) {
		GraveStoneWithNameplate graveStone = GraveStoneWithNameplate.builder()
				.timeOfDeath(gameModeState.getRoundRemainingTime().format(timeFormatter))
				.deadPlayerReference(reference)
				.deadPlayerRole(player.info().getCurrentRoundRole())
				.deadPlayerName(player.component().getDisplayName())
				.build();

		if (deathComponent.getDeathCause() != null) {
			DamageCause damageCause = DamageCause.valueOf(deathComponent.getDeathCause().getId().toUpperCase());
			graveStone.setCauseOfDeath(damageCause);
		}

		GraveSystem.spawnGraveAtPlayerDeath(world, graveStone, reference);
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
		var player = getPlayerFrom(reference).orElse(null);
		if (player == null) return;

		// Disable death screen
		deathComponent.setShowDeathMenu(false);
		CompletableFutureUtil._catch(DeathComponent.respawn(store, reference));

		World world = player.component().getWorld();
		if (world == null) return;

		GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());
		if (gameModeState == null || !RoundState.IN_GAME.equals(gameModeState.roundState)) {
			return;
		}

		world.execute(() -> {
			player.reference().getStore().ensureComponent(player.reference(), LostInCombat.componentType);
			player.component().getInventory().clear();
			SpectatorMode.setGameModeToSpectator(player);
			updatePlayerCountsOnPlayerDeath(player.refComponent(), player.info().getCurrentRoundRole(), gameModeState);
			player.info().getHud().update();

			updateAttackerKarma(deathComponent, player, gameModeState);
			spawnGraveStone(reference, deathComponent, gameModeState, player, world);

			if (roundShouldEnd(gameModeState)) {
				HytaleServer.get().getEventBus()
						.dispatchForAsync(FinishCurrentRoundEvent.class)
						.dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
			}
		});
	}

}
