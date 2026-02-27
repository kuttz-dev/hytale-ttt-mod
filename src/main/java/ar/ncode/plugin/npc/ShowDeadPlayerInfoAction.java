package ar.ncode.plugin.npc;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.component.DeadPlayerInfoComponent;
import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.ui.pages.GravePlatePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class ShowDeadPlayerInfoAction extends ActionBase {

	public ShowDeadPlayerInfoAction(@Nonnull BuilderActionBase builderActionBase) {
		super(builderActionBase);
	}

	public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
		return super.canExecute(ref, role, sensorInfo, dt, store) && role.getStateSupport().getInteractionIterationTarget() != null;
	}

	@Override
	public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
		super.execute(ref, role, sensorInfo, dt, store);

		var deadPlayerInfo = store.getComponent(ref, DeadPlayerInfoComponent.componentType);
		Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
		if (playerReference == null || deadPlayerInfo == null) {
			return false;
		}

		var player = PlayerAccessors.getPlayerFrom(playerReference, store);
		if (player.isEmpty()) {
			return false;
		}

		inspectDeadCorpse(deadPlayerInfo, playerReference, player.get());
		return true;
	}

	public static void inspectDeadCorpse(DeadPlayerInfoComponent deadPlayerInfo, Ref<EntityStore> playerReference, PlayerComponents player) {
		var playerRole = player.info().getCurrentRoundRole();
		giveRemainingCreditsToPlayerIfIsSpecialRole(deadPlayerInfo, player, playerRole);

		player.component().getPageManager().openCustomPage(
				playerReference, playerReference.getStore(),
				new GravePlatePage(player.refComponent(), CustomPageLifetime.CanDismiss, deadPlayerInfo)
		);
	}

	private static void giveRemainingCreditsToPlayerIfIsSpecialRole(DeadPlayerInfoComponent deadPlayerInfo, PlayerComponents player, CustomRole playerRole) {
		if (!player.info().isSpectator() && playerRole != null && playerRole.getStartingCredits() > 0) {
			player.info().setCredits(player.info().getCredits() + deadPlayerInfo.getCredits());
			deadPlayerInfo.setCredits(0);
		}
	}

	public static class Builder extends BuilderActionBase {
		@Nullable
		@Override
		public String getShortDescription() {
			return "Opens the GUI to see dead player info";
		}

		@Nullable
		@Override
		public String getLongDescription() {
			return "Opens the GUI to see dead player info";
		}

		@Nullable
		@Override
		public Action build(BuilderSupport builderSupport) {
			return new ShowDeadPlayerInfoAction(this);
		}

		@Nullable
		@Override
		public BuilderDescriptorState getBuilderDescriptorState() {
			return BuilderDescriptorState.Stable;
		}
	}
}
