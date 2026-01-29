package ar.ncode.plugin.system.scheduled;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static ar.ncode.plugin.commands.Shop.openShopIfUserHasRole;

public class DoubleTapDetector {

	private static final long TRIGGER_COOLDOWN_MS = 500L;
	private static final long TAP_MIN_MS = 10L;
	private static final long TAP_MAX_MS = 400L;
	private static final long DOUBLE_TAP_WINDOW_MS = 400L;
	private static DoubleTapDetector instance;
	private final Map<UUID, PlayerWalkState> playerStates = new ConcurrentHashMap();

	public static DoubleTapDetector getInstance() {
		if (instance == null) {
			instance = new DoubleTapDetector();
		}

		return instance;
	}

	public void tick() {
		Universe universe = Universe.get();
		if (universe == null) {
			return;
		}

		for (PlayerRef playerRef : universe.getPlayers()) {
			if (playerRef == null) {
				continue;
			}

			Ref<EntityStore> ref = playerRef.getReference();
			if (ref == null || !ref.isValid()) {
				continue;
			}

			Store<EntityStore> store = ref.getStore();
			if (store == null) {
				continue;
			}

			EntityStore entityStore = store.getExternalData();
			if (entityStore == null) {
				continue;
			}

			World world = entityStore.getWorld();
			if (world == null) {
				continue;
			}

			world.execute(() -> processPlayerTick(playerRef, ref, store, world));
		}
	}

	private void processPlayerTick(
			PlayerRef playerRef,
			Ref<EntityStore> ref,
			Store<EntityStore> store,
			World world
	) {
		if (ref == null || !ref.isValid()) {
			return;
		}

		Player player = ref.getStore().getComponent(ref, Player.getComponentType());
		PlayerGameModeInfo playerInfo = store.getComponent(ref, PlayerGameModeInfo.componentType);

		if (player == null || playerInfo == null) {
			return;
		}

		MovementStatesComponent movementComponent =
				store.getComponent(ref, MovementStatesComponent.getComponentType());
		if (movementComponent == null) {
			return;
		}

		MovementStates states = movementComponent.getMovementStates();
		if (states == null) {
			return;
		}

		UUID playerId = playerRef.getUuid();
		boolean currentlyWalking = states.walking;

		checkWalkStateChange(
				playerId,
				currentlyWalking,
				ref,
				player,
				playerRef,
				playerInfo
		);
	}

	private void checkWalkStateChange(
			UUID playerId,
			boolean currentlyWalking,
			Ref<EntityStore> ref,
			Player player,
			PlayerRef playerRef,
			PlayerGameModeInfo playerInfo
	) {
		long now = System.currentTimeMillis();

		PlayerWalkState state = playerStates.computeIfAbsent(
				playerId, k -> new PlayerWalkState()
		);

		// Cooldown after a successful trigger
		if (now - state.lastTriggerTime < TRIGGER_COOLDOWN_MS) {
			state.previousWalking = currentlyWalking;
			return;
		}

		boolean walkingChanged = state.previousWalking != currentlyWalking;

		if (walkingChanged) {
			if (!state.previousWalking && currentlyWalking) {
				// Walk started → tap begin
				state.tapStartTime = now;
			} else {
				// Walk stopped → tap end
				handleTapEnd(state, now, ref, player, playerRef, playerInfo);
			}

			state.previousWalking = currentlyWalking;
		}

		expireFirstTapIfNeeded(state, now);
	}

	private void handleTapEnd(
			PlayerWalkState state,
			long now,
			Ref<EntityStore> ref,
			Player player,
			PlayerRef playerRef,
			PlayerGameModeInfo playerInfo
	) {
		long holdDuration = now - state.tapStartTime;

		if (holdDuration < TAP_MIN_MS || holdDuration > TAP_MAX_MS) {
			state.firstTapCompleteTime = 0L;
			return;
		}

		// Valid tap
		if (state.firstTapCompleteTime > 0L
				&& now - state.firstTapCompleteTime <= DOUBLE_TAP_WINDOW_MS) {

			// Double tap detected
			state.firstTapCompleteTime = 0L;
			state.lastTriggerTime = now;
			openShopIfUserHasRole(ref, playerRef, player, playerInfo);

		} else {
			// First tap completed
			state.firstTapCompleteTime = now;
		}
	}

	private void expireFirstTapIfNeeded(PlayerWalkState state, long now) {
		if (state.firstTapCompleteTime > 0L
				&& now - state.firstTapCompleteTime > DOUBLE_TAP_WINDOW_MS) {
			state.firstTapCompleteTime = 0L;
		}
	}

	/**
	 * Removes a player's walk state from the detector.
	 * Should be called when a player disconnects to prevent memory leaks.
	 *
	 * @param playerId The UUID of the player to remove
	 */
	public void removePlayer(UUID playerId) {
		playerStates.remove(playerId);
	}

	/**
	 * Clears all player states from the detector.
	 * Should be called when transitioning to a new world instance to prevent memory leaks.
	 */
	public void clearAllPlayers() {
		playerStates.clear();
	}

	/**
	 * Returns the number of player states currently tracked.
	 * Useful for debugging memory leaks.
	 */
	public int getPlayerStateCount() {
		return playerStates.size();
	}

	private static class PlayerWalkState {
		boolean previousWalking = false;
		long tapStartTime = 0L;
		long firstTapCompleteTime = 0L;
		long lastTriggerTime = 0L;
	}
}
