package ar.ncode.plugin.system.player;

import ar.ncode.plugin.commands.SpectatorMode;
import ar.ncode.plugin.component.DeadPlayerInfoComponent;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.model.DamageCause;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoleGroup;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.system.DeathSystem;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import com.hypixel.hytale.common.util.CompletableFutureUtil;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
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
			DeathSystems.PlayerDeathScreen.class, OrderPriority.FURTHEST));

	public static void updatePlayerCountsOnPlayerDeath(PlayerRef playerRef, CustomRole role, GameModeState gameModeState) {
		gameModeState.spectators.add(playerRef.getUuid());

		if (role == null) {
			return;
		}

		if (RoleGroup.TRAITOR.equals(role.getRoleGroup())) {
			gameModeState.traitorsAlive.remove(playerRef.getUuid());

		} else if (RoleGroup.INNOCENT.equals(role.getRoleGroup())) {
			gameModeState.innocentsAlice.remove(playerRef.getUuid());
		}
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

	private static void updateAttackerKarma(@NonNullDecl DeathComponent deathComponent, PlayerComponents player, GameModeState gameModeState, ComponentAccessor<EntityStore> store) {
		if (deathComponent.getDeathInfo() == null) {
			return;
		}

		Damage.Source source = deathComponent.getDeathInfo().getSource();

		if (source instanceof Damage.EntitySource attackerRef) {
			var attacker = getPlayerFrom(attackerRef.getRef(), store).orElse(null);
			if (attacker == null) return;

			if (attacker.refComponent() != null && attacker.info() != null) {
				int value = calculateKarmaForAttacker(attacker.info().getCurrentRoundRole(), player.info().getCurrentRoundRole());
				gameModeState.karmaUpdates.merge(attacker.refComponent().getUuid(), value, Integer::sum);
			}
		}
	}

	private static void spawnGraveStone(@NonNullDecl DeathComponent deathComponent, GameModeState gameModeState, PlayerComponents player, World world) {
		DeadPlayerInfoComponent graveStone = DeadPlayerInfoComponent.builder()
				.timeOfDeath(gameModeState.getRoundRemainingTime().format(timeFormatter))
				.deadPlayerReference(player.reference())
				.deadPlayerRole(player.info().getCurrentRoundRole())
				.deadPlayerName(player.component().getDisplayName())
				.build();

		if (deathComponent.getDeathCause() != null) {
			DamageCause damageCause = DamageCause.valueOf(deathComponent.getDeathCause().getId().toUpperCase());
			graveStone.setCauseOfDeath(damageCause);
		}

		DeathSystem.spawnRemainsAtPlayerDeath(world, graveStone, player.reference(), player.reference().getStore());
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
		var player = getPlayerFrom(reference, commandBuffer).orElse(null);
		if (player == null) return;

		World world = player.component().getWorld();
		if (world == null) return;

		world.execute(() -> {
			// Disable death screen
			deathComponent.setShowDeathMenu(false);
			deathComponent.setItemsLossMode(DeathConfig.ItemsLossMode.ALL);
			deathComponent.setItemsDurabilityLossPercentage(0.0F);
			CompletableFutureUtil._catch(DeathComponent.respawn(commandBuffer, reference));

			GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());
			if (gameModeState == null || !RoundState.IN_GAME.equals(gameModeState.roundState)) {
				return;
			}

			commandBuffer.ensureComponent(player.reference(), LostInCombat.componentType);
			SpectatorMode.setGameModeToSpectator(player, commandBuffer);
			updatePlayerCountsOnPlayerDeath(player.refComponent(), player.info().getCurrentRoundRole(), gameModeState);
			player.component().getInventory().clear();
			player.info().getHud().update();

			updateAttackerKarma(deathComponent, player, gameModeState, commandBuffer);

			spawnGraveStone(deathComponent, gameModeState, player, world);

			if (roundShouldEnd(gameModeState)) {
				HytaleServer.get().getEventBus()
						.dispatchForAsync(FinishCurrentRoundEvent.class)
						.dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
			}
		});
	}

}
