package ar.ncode.plugin.system.scheduled;

import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.system.event.FinishCurrentRoundEvent;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.time.LocalTime;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.TranslationKey.ROUND_TIME_FINISHED;

public class WorldRoundTimeSystem extends TickingSystem<EntityStore> {

	float elapsedTime = 0;

	@Override
	public void tick(float dt, int index, @NonNullDecl Store<EntityStore> store) {
		elapsedTime += dt;
		if (elapsedTime < 1) {
			return;
		}
		elapsedTime = 0;

		World world = store.getExternalData().getWorld();
		GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());
		if (gameModeState == null || !RoundState.IN_GAME.equals(gameModeState.roundState)) {
			return;
		}

		LocalTime remainingTime = gameModeState.getRoundRemainingTime();
		if (remainingTime.getMinute() > 0 || remainingTime.getSecond() > 0) {
			return;
		}

		if (RoundState.IN_GAME.equals(gameModeState.roundState)) {
			Message message = Message.translation(ROUND_TIME_FINISHED.get());
			EventTitleUtil.showEventTitleToWorld(
					message,
					Message.raw(""),
					true, "ui/icons/EntityStats/Sword_Icon.png",
					4.0f, 1.5f, 1.5f,
					store
			);

			HytaleServer.get().getEventBus()
					.dispatchForAsync(FinishCurrentRoundEvent.class)
					.dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
		}
	}
}
