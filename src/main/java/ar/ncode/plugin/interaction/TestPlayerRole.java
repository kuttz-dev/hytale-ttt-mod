package ar.ncode.plugin.interaction;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.model.enums.RoleGroup;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class TestPlayerRole extends SimpleInstantInteraction {

	public static final BuilderCodec<TestPlayerRole> CODEC = BuilderCodec.builder(
			TestPlayerRole.class, TestPlayerRole::new, SimpleInstantInteraction.CODEC
	).build();

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final double SCAN_DISTANCE = 1.5;

	@Override
	protected void firstRun(
			@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext ctx,
			@NonNullDecl CooldownHandler cooldownHandler
	) {
		CommandBuffer<EntityStore> commandBuffer = ctx.getCommandBuffer();
		if (commandBuffer == null) {
			ctx.getState().state = InteractionState.Failed;
			LOGGER.atInfo().log("CommandBuffer is null");
			return;
		}

		BlockPosition targetBlock = ctx.getTargetBlock();
		if (targetBlock == null) {
			ctx.getState().state = InteractionState.Failed;
			return;
		}

		Vector3d blockPosition = new Vector3d(targetBlock.x, targetBlock.y, targetBlock.z);

		SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
				commandBuffer.getResource(EntityModule.get().getPlayerSpatialResourceType());
		SpatialStructure<Ref<EntityStore>> spatialStructure = playerSpatialResource.getSpatialStructure();

		Ref<EntityStore> closest = spatialStructure.closest(blockPosition);
		if (closest == null || !closest.isValid()) {
			LOGGER.atInfo().log("No player found near scanner");
			return;
		}

		World world = commandBuffer.getExternalData().getWorld();
		Store<EntityStore> store = world.getEntityStore().getStore();

		world.execute(() -> {
			var transformComponent = commandBuffer.getComponent(closest, TransformComponent.getComponentType());
			if (transformComponent == null) {
				return;
			}

			var playerOpt = PlayerAccessors.getPlayerFrom(closest);
			if (playerOpt.isEmpty()) {
				return;
			}

			var player = playerOpt.get();
			var distance = blockPosition.distanceTo(transformComponent.getPosition());

			// Check if player is within scanner range
			if (distance > SCAN_DISTANCE) {
				LOGGER.atInfo().log("Player too far from scanner: " + distance);
				return;
			}

			// Get player's role
			var role = player.info().getCurrentRoundRole();
			if (role == null || role.getRoleGroup() == null) {
				LOGGER.atInfo().log("Player has no role assigned");
				return;
			}

			// Determine if player is traitor
			boolean isTraitor = role.getRoleGroup() == RoleGroup.TRAITOR;
			String effectId = isTraitor ? "Scanner_Red" : "Scanner_Green";

			// Apply visual effect to the scanned player
			applyEffectToPlayer(closest, effectId, store);

			// Broadcast scan result to nearby players
			String resultMessage = isTraitor
					? "[SCANNER] TRAITOR DETECTED!"
					: "[SCANNER] Cleared - Innocent";

			// Send result to operator (who clicked the scanner)
			var operatorRef = ctx.getOwningEntity();
			if (operatorRef != null && operatorRef.isValid()) {
				var operatorOpt = PlayerAccessors.getPlayerFrom(operatorRef);
				operatorOpt.ifPresent(op ->
						op.component().sendMessage(Message.raw(resultMessage))
				);
			}

			LOGGER.atInfo().log("Scanner result for " + player.component().getDisplayName() + ": " + role.getId());
		});
	}

	private void applyEffectToPlayer(Ref<EntityStore> playerRef, String effectId, Store<EntityStore> store) {
		EffectControllerComponent effects = store.getComponent(
				playerRef, EffectControllerComponent.getComponentType());

		if (effects == null) {
			LOGGER.atWarning().log("Player has no EffectControllerComponent");
			return;
		}

		EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
		if (effect == null) {
			LOGGER.atWarning().log("Effect not found: " + effectId);
			return;
		}

		effects.addEffect(playerRef, effect, store);
		LOGGER.atInfo().log("Applied effect: " + effectId);
	}
}
