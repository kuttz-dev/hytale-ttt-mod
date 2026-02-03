package ar.ncode.plugin.accessors;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.PlayerComponents;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;

public class PlayerAccessors {

	public static Optional<PlayerComponents> getPlayerFrom(Ref<EntityStore> reference) {
		if (!reference.isValid()) {
			return Optional.empty();
		}

		var store = reference.getStore();
		Player player = store.getComponent(reference, Player.getComponentType());
		PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());
		PlayerGameModeInfo playerInfo = store.getComponent(reference, PlayerGameModeInfo.componentType);

		if (player == null || playerRef == null || playerInfo == null) {
			return Optional.empty();
		}

		return Optional.of(
				new PlayerComponents(player, playerRef, playerInfo, reference)
		);
	}
}
