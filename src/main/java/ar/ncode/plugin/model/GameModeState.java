package ar.ncode.plugin.model;

import ar.ncode.plugin.component.DeadPlayerInfoComponent;
import ar.ncode.plugin.model.enums.RoundState;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;

@Getter
@Setter
@ToString
public class GameModeState {

	public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("mm:ss");
	public RoundState roundState = RoundState.PREPARING;
	public Set<UUID> traitorsAlive = ConcurrentHashMap.newKeySet();
	public Set<UUID> innocentsAlive = ConcurrentHashMap.newKeySet();
	public Set<UUID> spectators = ConcurrentHashMap.newKeySet();
	public Map<UUID, Integer> karmaUpdates = new HashMap<>();
	public Map<UUID, Integer> killUpdates = new HashMap<>();
	public Map<UUID, Integer> deathsUpdates = new HashMap<>();
	public LocalDateTime roundStateUpdatedAt;
	public List<DeadPlayerInfoComponent> graveStones = new ArrayList<>();
	public List<Ref<EntityStore>> corpses = new ArrayList<>();
	public int playedRounds = 0;
	public Map<String, Integer> mapVotes = new HashMap<>();
	@Accessors(fluent = true)
	private boolean playersAreVotingMap = false;
	@Accessors(fluent = true)
	private boolean mapIsAboutToChange = false;

	public void addGraveStone(DeadPlayerInfoComponent graveStone) {
		this.graveStones.add(graveStone);
	}

	public Duration getElapsedTimeSinceRoundUpdate() {
		if (roundStateUpdatedAt == null) {
			return Duration.of(0, ChronoUnit.SECONDS);
		}

		LocalDateTime currentDateTime = LocalDateTime.now();
		return Duration.between(roundStateUpdatedAt, currentDateTime);
	}

	public LocalTime getRemainingTime(RoundState roundState, boolean playersAreVotingMap, boolean mapIsAboutToChange) {
		Duration roundElapsedTime = getElapsedTimeSinceRoundUpdate();

		int stateDuration = 0;
		if (RoundState.IN_GAME.equals(roundState)) {
			stateDuration = config.get().getRoundDurationInSeconds();

		} else if (RoundState.PREPARING.equals(roundState)) {
			stateDuration = config.get().getTimeBeforeRoundInSeconds();

		} else if (RoundState.AFTER_GAME.equals(roundState) && playersAreVotingMap) {
			stateDuration = config.get().getTimeToVoteMapInSeconds();

		}  else if (RoundState.AFTER_GAME.equals(roundState) && mapIsAboutToChange) {
			stateDuration = config.get().getTimeBeforeChangingMapInSeconds();

		} else {
			stateDuration = config.get().getTimeAfterRoundInSeconds();
		}

		long remainingTime = stateDuration - roundElapsedTime.toSeconds();

		long minutes = remainingTime / 60; // Remainder minutes after full hours
		long seconds = remainingTime % 60; // Remainder seconds after full minutes

		if (minutes < 0) {
			minutes = 0;
		}

		if (seconds < 0) {
			seconds = 0;
		}

		return LocalTime.of(0, (int) minutes, (int) seconds);
	}

	public boolean hasLastRoundFinished() {
		return playedRounds == config.get().getRoundsPerMap();
	}

	public void addVoteForMap(String mapName) {
		this.mapVotes.compute(mapName, (key, v) -> v == null ? 1 : v + 1);
	}

	public void updateRoundState(RoundState state) {
		roundState = state;
		roundStateUpdatedAt = LocalDateTime.now();
	}

	public void updateRoundState(RoundState state, boolean playersAreVotingMap, boolean mapIsAboutToChange) {
		this.playersAreVotingMap = playersAreVotingMap;
		this.mapIsAboutToChange = mapIsAboutToChange;
		updateRoundState(state);
	}

}
