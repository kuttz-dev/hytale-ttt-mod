package ar.ncode.plugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * This is an example command that will simply print the name of the plugin in chat when used.
 */
public class ExampleCommand extends CommandBase {

	private final String pluginName;
	private final String pluginVersion;

	RequiredArg<String> messageArg = this.withRequiredArg("effect", "Argument Description", ArgTypes.STRING);

	public ExampleCommand(String pluginName, String pluginVersion) {
		super("test", "Prints a test message from the " + pluginName + " plugin.");
		this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
		this.pluginName = pluginName;
		this.pluginVersion = pluginVersion;
	}

	@Override
	protected void executeSync(@Nonnull CommandContext ctx) {
		ctx.sendMessage(Message.raw("Hello from the " + pluginName + " v" + pluginVersion + " plugin!"));
		if (!ctx.isPlayer()) {
			return;
		}

		String message = messageArg.get(ctx); // get the argument text by the component+
		CommandSender sender = ctx.sender();

		if (sender instanceof Player player) {
			Ref<EntityStore> reference = player.getReference();
			PageManager pageManager = player.getPageManager();
			if (reference == null) return;


			World world = player.getWorld();
			world.execute(() -> {
				//var asd = reference.getStore().removeComponentIfExists(reference, EntityTrackerSystems.Visible.getComponentType());
				var asd = reference.getStore().addComponent(reference, Intangible.getComponentType());
				LOGGER.atInfo().log("Effect applied: " + asd);
//                PlayerRef refComponent = world.getEntityStore().getStore().getComponent(reference, PlayerRef.getComponentType());
//                EffectControllerComponent controller = reference.getStore().getComponent(reference, EffectControllerComponent.getComponentType());
//                // Get effect from asset store
//                EntityEffect effect = EntityEffect.getAssetMap().getAsset(message);
//                // Apply effect
//                boolean applied = false;
//                if (effect != null) {
//                    applied = controller.addEffect(reference, effect, reference.getStore());
//                    LOGGER.atInfo().log("Effect encontrado: " + effect);
//                }
//                LOGGER.atInfo().log("Effect applied: " + applied);
			});
		}
	}
}