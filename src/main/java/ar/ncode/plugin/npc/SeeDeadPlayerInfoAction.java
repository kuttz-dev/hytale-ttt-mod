package ar.ncode.plugin.npc;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.component.DeadPlayerInfoComponent;
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

public class SeeDeadPlayerInfoAction extends ActionBase {

	public SeeDeadPlayerInfoAction(@Nonnull BuilderActionBase builderActionBase) {
		super(builderActionBase);
	}

	public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
		return super.canExecute(ref, role, sensorInfo, dt, store) && role.getStateSupport().getInteractionIterationTarget() != null;
	}

	@Override
	public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
		super.execute(ref, role, sensorInfo, dt, store);

		var deadPlayerInfo = ref.getStore().getComponent(ref, DeadPlayerInfoComponent.componentType);
		Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
		if (playerReference == null) {
			return false;
		}

		var player = PlayerAccessors.getPlayerFrom(playerReference, playerReference.getStore());
		if (player.isEmpty()) {
			return false;
		}

		player.get().component().getPageManager().openCustomPage(
				playerReference, playerReference.getStore(),
				new GravePlatePage(player.get().refComponent(), CustomPageLifetime.CanDismiss, deadPlayerInfo)
		);
		return true;
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
			return new SeeDeadPlayerInfoAction(this);
		}

		@Nullable
		@Override
		public BuilderDescriptorState getBuilderDescriptorState() {
			return BuilderDescriptorState.Stable;
		}
	}
}
