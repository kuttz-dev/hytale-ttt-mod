package ar.ncode.plugin.commands;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.ui.pages.MapVotePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.worldPreviews;

public class MapVote extends CommandBase {

	public MapVote() {
		super("vote", "Command to open the map voting menu.");
		super.addAliases("votemap");
	}

	private static void openGuiForPlayer(@NonNullDecl CommandContext ctx, Ref<EntityStore> reference) {
		var playerInfo = reference.getStore().getComponent(reference, PlayerGameModeInfo.componentType);
		var player = reference.getStore().getComponent(reference, Player.getComponentType());
		var playerRef = reference.getStore().getComponent(reference, PlayerRef.getComponentType());

		if (playerInfo == null || player == null || playerRef == null) {
			ctx.sendMessage(Message.raw("An error occurred while trying to access your player information."));
			return;
		}

		World world = player.getWorld();
		if (world == null) {
			ctx.sendMessage(Message.raw("An error occurred while trying to access your world information."));
			return;
		}

		GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());

		if (!gameModeState.hasLastRoundFinished()) {
			ctx.sendMessage(Message.raw("You can only use this command after the last round has finished."));
			return;
		}

		player.getPageManager().openCustomPage(
				reference, reference.getStore(), new MapVotePage(playerRef, CustomPageLifetime.CanDismiss, worldPreviews,
						playerInfo)
		);
	}

	@Override
	protected void executeSync(@NonNullDecl CommandContext ctx) {
		Ref<EntityStore> reference = ctx.senderAsPlayerRef();

		if (reference == null || !reference.isValid()) {
			ctx.sendMessage(Message.raw("You can't use this command from the console."));
			return;
		}

		var world = reference.getStore().getExternalData().getWorld();

		world.execute(() -> openGuiForPlayer(ctx, reference));
	}
}
