package ar.ncode.plugin.interaction;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.model.enums.RoleGroup;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.List;

public class TestPlayerRolePotion extends SimpleInstantInteraction {

	public static final BuilderCodec<TestPlayerRolePotion> CODEC = BuilderCodec.builder(
			TestPlayerRolePotion.class, TestPlayerRolePotion::new, SimpleInstantInteraction.CODEC
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

		World world = commandBuffer.getExternalData().getWorld();

		world.execute(() -> {
			var playerOpt = PlayerAccessors.getPlayerFrom(ctx.getOwningEntity());
			var targetPlayerTransform = commandBuffer.getComponent(ctx.getOwningEntity(), TransformComponent.getComponentType());

			if (playerOpt.isEmpty() || targetPlayerTransform == null) {
				return;
			}

			var player = playerOpt.get();

			// Get player's role
			var role = player.info().getCurrentRoundRole();
			if (role == null || role.getRoleGroup() == null) {
				LOGGER.atInfo().log("Player has no role assigned");
				return;
			}

			// Determine if player is traitor
			boolean isTraitor = role.getRoleGroup() == RoleGroup.TRAITOR;
			String effect = isTraitor ? "TTT_Potion_Veritaserum_Particles_Traitor" : "TTT_Potion_Veritaserum_Particles_Innocent";

			ParticleUtil.spawnParticleEffect(
					effect,
					targetPlayerTransform.getPosition().getX(),
					targetPlayerTransform.getPosition().getY(),
					targetPlayerTransform.getPosition().getZ(),
					targetPlayerTransform.getRotation().getYaw(),
					targetPlayerTransform.getRotation().getPitch(),
					targetPlayerTransform.getRotation().getRoll(),
					1f,
					new Color(Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE),
					null,
					List.of(player.reference()),
					world.getEntityStore().getStore()
			);

			// Send result to operator (who clicked the scanner)
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
