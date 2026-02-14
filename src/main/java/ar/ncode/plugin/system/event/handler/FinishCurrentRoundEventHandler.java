package ar.ncode.plugin.system.event.handler;

import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.system.GameModeSystem;
import ar.ncode.plugin.system.event.FinishCurrentMapEvent;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import ar.ncode.plugin.system.event.StartNewRoundEvent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;

public class FinishCurrentRoundEventHandler implements Consumer<FinishCurrentRoundEvent> {

	public static boolean roundShouldEnd(GameModeState gameModeState) {
		return gameModeState.innocentsAlice.isEmpty() || gameModeState.traitorsAlive.isEmpty();
	}


	@Override
	public void accept(FinishCurrentRoundEvent event) {
		World world = Universe.get().getWorld(event.getWorldUUID());
		if (world == null) return;

		GameModeState gameModeState = gameModeStateForWorld.get(event.getWorldUUID());
		if (gameModeState == null || !roundShouldEnd(gameModeState)) return;

		GameModeSystem.INSTANCE.doAfterRound(world, gameModeState);

		if (gameModeState.hasLastRoundFinished()) {
			HytaleServer.get().getEventBus()
					.dispatchForAsync(FinishCurrentMapEvent.class)
					.dispatch(new FinishCurrentMapEvent(world.getWorldConfig().getUuid()));

		} else {
			// A new round has to begin
			HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
						// Check if world is still alive before executing (prevents memory leak from stale references)
						if (!world.isAlive()) return;
						HytaleServer.get().getEventBus()
								.dispatchForAsync(StartNewRoundEvent.class)
								.dispatch(new StartNewRoundEvent(world.getWorldConfig().getUuid()));
					},
					config.get().getTimeAfterRoundInSeconds(),
					TimeUnit.SECONDS
			);
		}
	}

}
