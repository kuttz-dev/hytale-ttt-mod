package ar.ncode.plugin.ecs.interaction;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.ecs.component.DeadPlayerGravestoneComponent;
import ar.ncode.plugin.ecs.component.DeadPlayerInfoComponent;
import ar.ncode.plugin.ecs.npc.ShowDeadPlayerInfoAction;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ShowDeadPlayerInfoInteraction extends SimpleInstantInteraction {

	public static final BuilderCodec<ShowDeadPlayerInfoInteraction> CODEC = BuilderCodec.builder(
			ShowDeadPlayerInfoInteraction.class,
			ShowDeadPlayerInfoInteraction::new, SimpleInstantInteraction.CODEC
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
		DeadPlayerGravestoneComponent graveStone = WorldAccessors.getBlockComponentAt(world, blockPosition, DeadPlayerGravestoneComponent.componentType);

		if (graveStone == null) {
			return;
		}

		Ref<EntityStore> reference = interactionContext.getEntity();
		world.execute(() -> {
			var player = PlayerAccessors.getPlayerFrom(reference, reference.getStore());

			if (player.isEmpty()) {
				interactionContext.getState().state = InteractionState.Failed;
				return;
			}

			DeadPlayerInfoComponent deadPlayerInfoComponent = graveStone.getDeadPlayerInfoComponent();
			ShowDeadPlayerInfoAction.inspectDeadCorpse(deadPlayerInfoComponent, reference, player.get());
		});
	}

}
