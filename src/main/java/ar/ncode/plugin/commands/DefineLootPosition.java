package ar.ncode.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.lootBox;

public class DefineLootPosition extends CommandBase {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public DefineLootPosition() {
		super("loot", "Adds a loot position at the player's current location.");
	}

	@Override
	protected void executeSync(@NonNullDecl CommandContext ctx) {
		Ref<EntityStore> reference = ctx.senderAsPlayerRef();

		if (reference == null || !reference.isValid()) {
			ctx.sendMessage(Message.raw("You can't use this command from the console."));
			return;
		}

		var world = reference.getStore().getExternalData().getWorld();
		world.execute(() -> {
			var transformComponent = reference.getStore().getComponent(reference, TransformComponent.getComponentType());
			if (transformComponent == null) {
				ctx.sendMessage(Message.raw("An error occurred while trying to access your player information."));
				return;
			}

			// Here you would add the logic to actually store the loot position
			lootBox.get().setPosition(transformComponent.getPosition());
			lootBox.get().setRotation(transformComponent.getRotation());
			lootBox.save();
		});
	}
}
