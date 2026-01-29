package ar.ncode.plugin.system.event;

import com.hypixel.hytale.event.IAsyncEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class MapEndEvent implements IAsyncEvent<UUID> {

	private final UUID oldWorldUUID;

}
