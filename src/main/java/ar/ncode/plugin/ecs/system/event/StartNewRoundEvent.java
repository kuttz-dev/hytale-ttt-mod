package ar.ncode.plugin.ecs.system.event;

import com.hypixel.hytale.event.IAsyncEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class StartNewRoundEvent implements IAsyncEvent<UUID> {

	private final UUID worldUUID;

}
