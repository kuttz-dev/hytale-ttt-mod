package ar.ncode.plugin.system.event.listener;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class SpectatorModeDamageListener extends DamageEventSystem {

	public static final Query<EntityStore> QUERY = Query.any();

	@Override
	public SystemGroup<EntityStore> getGroup() {
		return DamageModule.get().getFilterDamageGroup();
	}

	@Override
	public void handle(int index,
	                   @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store,
	                   @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl Damage damage
	) {
		// Skip if already cancelled by another system
		if (damage.isCancelled()) {
			return;
		}

		// Get references to the attacker
		if (!(damage.getSource() instanceof Damage.EntitySource)) {
			return;
		}
		Ref<EntityStore> attackerRef = ((Damage.EntitySource) damage.getSource()).getRef();

		// Check if attacker is a component
		PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
		if (attackerPlayerRef == null) {
			return;
		}

		PlayerGameModeInfo playerGameModeInfo = store.getComponent(attackerRef, PlayerGameModeInfo.componentType);
		if (playerGameModeInfo == null) {
			return;
		}

		if (playerGameModeInfo.isSpectator()) {
			damage.setCancelled(true);
		}
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return QUERY;
	}
}
