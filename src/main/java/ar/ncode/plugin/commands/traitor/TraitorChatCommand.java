package ar.ncode.plugin.commands.traitor;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.TranslationKey;
import ar.ncode.plugin.model.enums.RoleGroup;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;

public class TraitorChatCommand extends AbstractAsyncCommand {

	RequiredArg<String> message = this.withRequiredArg(
			"message", TranslationKey.TRAITORS_CHAT_COMMAND_DESCRIPTION.get(),
			ArgTypes.STRING
	);

	public TraitorChatCommand() {
		super("t", "description");
	}

	@NonNullDecl
	@Override
	protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
		return CompletableFuture.runAsync(() -> executeSync(commandContext));
	}

	protected void executeSync(@NonNullDecl CommandContext ctx) {
		if (!ctx.isPlayer() || ctx.senderAsPlayerRef() == null) {
			ctx.sendMessage(Message.raw("Command can only be used by players."));
			return;
		}

		World world = ctx.senderAs(Player.class).getWorld();
		Ref<EntityStore> ref = ctx.senderAsPlayerRef();
		Player player = ctx.senderAs(Player.class);

		world.execute(() -> {
			PlayerGameModeInfo playerInfo = ref.getStore().getComponent(ref, PlayerGameModeInfo.componentType);

			if (playerInfo == null) {
				return;
			}

			if (!RoleGroup.TRAITOR.equals(playerInfo.getCurrentRoundRole().getRoleGroup())) {
				ctx.sendMessage(Message.translation(TranslationKey.TRAITORS_CHAT_ONLY_FOR_TRAITORS.get()));
				return;
			}

			String chatMessage = ctx.get(this.message);

			if (player.getWorld() == null) {
				ctx.sendMessage(Message.translation(TranslationKey.TRAITORS_CHAT_ONLY_FOR_TRAITORS.get()));
				return;
			}

			GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());
			for (UUID playerUUID : gameModeState.traitorsAlive) {
				PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
				if (playerRef == null) {
					continue;
				}

				playerRef.sendMessage(
						Message.join(
								Message.translation(TranslationKey.TRAITORS_CHAT_PREFIX.get())
										.color(RoleGroup.TRAITOR.guiColor),
								Message.raw(" - " + player.getDisplayName() + ": " + chatMessage)
						)
				);
			}
		});
	}

}
