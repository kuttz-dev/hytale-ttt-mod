package ar.ncode.plugin.model;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record PlayerComponents(
		Player component,
		PlayerRef refComponent,
		PlayerGameModeInfo info,
		Ref<EntityStore> reference
) {
}