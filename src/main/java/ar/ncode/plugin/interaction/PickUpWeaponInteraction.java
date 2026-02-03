package ar.ncode.plugin.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PickUpWeaponInteraction extends SimpleInstantInteraction {

	public static final BuilderCodec<PickUpWeaponInteraction> CODEC = BuilderCodec.builder(PickUpWeaponInteraction.class,
					PickUpWeaponInteraction::new, SimpleInstantInteraction.CODEC)
			.build();
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	@Override
	protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {
		interactionContext.getState().state = InteractionState.Failed;
		LOGGER.atInfo().log("pickup interaction");
	}

}
