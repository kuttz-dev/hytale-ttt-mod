package ar.ncode.plugin.commands;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.component.death.LostInCombat;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

import static ar.ncode.plugin.model.CustomPermissions.TTT_INFO_SEE;

public class PlayerInfoCommand extends AbstractAsyncCommand {

	RequiredArg<PlayerRef> playerArg = this.withRequiredArg("targetPlayer", "Target player to see its info", ArgTypes.PLAYER_REF);

	public PlayerInfoCommand() {
		super("info", "Shows the player info related to the game mode.");
		requirePermission(TTT_INFO_SEE);
	}

	@Nonnull
	@Override
	protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
		return CompletableFuture.runAsync(() -> {
			var playerRef = ctx.get(playerArg);
			Ref<EntityStore> reference = playerRef.getReference();

			if (reference == null || !reference.isValid() || playerRef.getWorldUuid() == null) {
				ctx.sendMessage(Message.raw("Player must be online and in a world"));
				return;
			}

			var world = Universe.get().getWorld(playerRef.getWorldUuid());
			if (world == null) {
				ctx.sendMessage(Message.raw("Player must be in a valid world"));
				return;
			}

			world.execute(() -> {
				var player = PlayerAccessors.getPlayerFrom(playerRef, reference.getStore());

				if (player.isEmpty()) {
					ctx.sendMessage(Message.raw("Could not obtain player info"));
					return;
				}

				boolean isLostInCombat = reference.getStore().getComponent(reference, LostInCombat.componentType) != null;
				boolean hasConfirmedDeath = reference.getStore().getComponent(reference, ConfirmedDeath.componentType) != null;
				boolean isIntangible = reference.getStore().getComponent(reference, Intangible.getComponentType()) != null;
				boolean isInvulnerable = reference.getStore().getComponent(reference, Invulnerable.getComponentType()) != null;

				PlayerGameModeInfo info = player.get().info();
				ctx.sendMessage(Message.raw("Player: " + player.get().component().getDisplayName()));
				if (info != null) {
					ctx.sendMessage(Message.raw("- role: " + info.getCurrentRoundRole().getId()));
					ctx.sendMessage(Message.raw("- role group: " + info.getCurrentRoundRole().getRoleGroup().name()));
					ctx.sendMessage(Message.raw("- is spectator: " + info.isSpectator()));
					ctx.sendMessage(Message.raw("- kda: " + info.getKills() + " " + info.getDeaths() + " 0"));
					ctx.sendMessage(Message.raw("- karma: " + info.getKarma()));
					ctx.sendMessage(Message.raw("- already voted map: " + info.hasAlreadyVotedMap()));
				}
				ctx.sendMessage(Message.raw("- is lost in combat: " + isLostInCombat));
				ctx.sendMessage(Message.raw("- has confirmed death: " + hasConfirmedDeath));
				ctx.sendMessage(Message.raw("- is invulnerable: " + isInvulnerable));
			});
		});
	}
}
