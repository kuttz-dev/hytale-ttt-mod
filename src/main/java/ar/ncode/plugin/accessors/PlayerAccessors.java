package ar.ncode.plugin.accessors;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.PlayerComponents;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerAccessors {

	public static Optional<PlayerComponents> getPlayerFrom(@Nullable Ref<EntityStore> reference, ComponentAccessor<EntityStore> store) {
		if (reference == null || !reference.isValid()) {
			return Optional.empty();
		}

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

	public static Optional<PlayerComponents> getPlayerFrom(@Nullable PlayerRef playerRef, ComponentAccessor<EntityStore> store) {
		if (playerRef == null || !playerRef.isValid() || playerRef.getReference() == null) {
			return Optional.empty();
		}

		Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
		PlayerGameModeInfo playerInfo = store.getComponent(playerRef.getReference(), PlayerGameModeInfo.componentType);

		if (player == null || playerInfo == null) {
			return Optional.empty();
		}

		return Optional.of(
				new PlayerComponents(player, playerRef, playerInfo, playerRef.getReference())
		);
	}
}
