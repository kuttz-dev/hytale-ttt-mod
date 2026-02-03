package ar.ncode.plugin.interaction;

import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.component.GraveStoneWithNameplate;
import ar.ncode.plugin.ui.pages.GravePlatePage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ShowDeadPlayerInteraction extends SimpleInstantInteraction {

	public static final BuilderCodec<ShowDeadPlayerInteraction> CODEC = BuilderCodec.builder(
			ShowDeadPlayerInteraction.class,
			ShowDeadPlayerInteraction::new, SimpleInstantInteraction.CODEC
	).build();

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	@Override
	protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		if (commandBuffer == null) {
			interactionContext.getState().state = InteractionState.Failed;
			LOGGER.atInfo().log("CommandBuffer is null");
			return;
		}

		World world = commandBuffer.getExternalData().getWorld();

		BlockPosition targetBlock = interactionContext.getTargetBlock();
		if (targetBlock == null) {
			interactionContext.getState().state = InteractionState.Failed;
			return;
		}
		Vector3i blockPosition = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
		GraveStoneWithNameplate graveStone = WorldAccessors.getBlockComponentAt(world, blockPosition,
				GraveStoneWithNameplate.componentType);

		if (graveStone == null) {
			return;
		}

		Ref<EntityStore> reference = interactionContext.getEntity();

		world.execute(() -> {
			Player player = reference.getStore().getComponent(reference, Player.getComponentType());
			PlayerRef playerRef = reference.getStore().getComponent(reference, PlayerRef.getComponentType());

			if (player == null || playerRef == null) {
				interactionContext.getState().state = InteractionState.Failed;
				return;
			}

			player.getPageManager().openCustomPage(reference, reference.getStore(), new GravePlatePage(playerRef,
					CustomPageLifetime.CanDismiss, graveStone));
		});
	}

}
