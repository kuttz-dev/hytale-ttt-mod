package ar.ncode.plugin.commands;

import ar.ncode.plugin.model.LootBox;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.instanceConfig;

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
			LootBox lootBox = new LootBox(transformComponent.getPosition(), transformComponent.getRotation());
			LootBox[] lootBoxes = instanceConfig.get().getLootBoxes();
			if (lootBoxes == null) {
				lootBoxes = new LootBox[0];
			}
			LootBox[] newLootBoxes = new LootBox[lootBoxes.length + 1];
			System.arraycopy(lootBoxes, 0, newLootBoxes, 0, lootBoxes.length);
			newLootBoxes[lootBoxes.length] = lootBox;
			instanceConfig.get().setLootBoxes(newLootBoxes);
			instanceConfig.save();
		});
	}
}
