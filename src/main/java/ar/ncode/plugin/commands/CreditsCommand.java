package ar.ncode.plugin.commands;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoleGroup;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.CustomPermissions.TTT_ADMIN_GROUP;
import static ar.ncode.plugin.model.CustomPermissions.TTT_CREDITS_SET;

public class CreditsCommand extends AbstractCommandCollection {

	public CreditsCommand() {
		super("credits", "Command for setting roles");
		setPermissionGroups(TTT_ADMIN_GROUP);
		this.addSubCommand(new SetCreditsCommand());
	}

	public static class SetCreditsCommand extends CommandBase {

		OptionalArg<PlayerRef> playerArg = this.withOptionalArg("targetPlayer", "Target player to change its role", ArgTypes.PLAYER_REF);
		RequiredArg<Integer> creditsArg = this.withRequiredArg("credits", "Amount of credits to be set", ArgTypes.INTEGER);

		public SetCreditsCommand() {
			super("set", "Sets a player role");
			requirePermission(TTT_CREDITS_SET);
		}

		@Override
		protected void executeSync(@Nonnull CommandContext ctx) {
			Integer desiredCreditsAmount = creditsArg.get(ctx);

			if (!ctx.isPlayer() || ctx.senderAsPlayerRef() == null) {
				ctx.sendMessage(Message.raw("Command can only be used by players."));
				return;
			}

			World world = ctx.senderAs(Player.class).getWorld();

			world.execute(() -> {

				var playerRef = playerArg.get(ctx);
				PlayerComponents player = null;
				if (playerRef == null) {
					player = PlayerAccessors.getPlayerFrom(ctx.senderAsPlayerRef(), ctx.senderAsPlayerRef().getStore()).orElse(null);

				} else {
					player = PlayerAccessors.getPlayerFrom(playerRef.getReference(), playerRef.getReference().getStore()).orElse(null);
				}

				if (player == null) {
					ctx.sendMessage(Message.raw("Player reference could not be obtained."));
					return;
				}

				GameModeState gameModeState = gameModeStateForWorld.get(world.getWorldConfig().getUuid());
				if (gameModeState == null) {
					ctx.sendMessage(Message.raw("TODO: Add error message"));
					return;
				}

				if (!player.info().isSpectator()) {
					if (RoleGroup.TRAITOR.equals(player.info().getCurrentRoundRole().getRoleGroup())) {
						gameModeState.traitorsAlive.remove(player.refComponent().getUuid());

					} else if (RoleGroup.INNOCENT.equals(player.info().getCurrentRoundRole().getRoleGroup())) {
						gameModeState.innocentsAlive.remove(player.refComponent().getUuid());
					}

				} else {
					gameModeState.spectators.remove(player.refComponent().getUuid());
					player.info().setSpectator(false);
				}

				player.info().setCredits(desiredCreditsAmount);
			});

		}
	}


}
