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

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.CustomPermissions.TTT_ADMIN_GROUP;
import static ar.ncode.plugin.model.CustomPermissions.TTT_ROLE_SET;

public class RoleCommand extends AbstractCommandCollection {

	public RoleCommand() {
		super("role", "Command for setting roles");
		setPermissionGroups(TTT_ADMIN_GROUP);
		this.addSubCommand(new SetRoleCommand());
	}

	public static class SetRoleCommand extends CommandBase {

		OptionalArg<PlayerRef> playerArg = this.withOptionalArg("targetPlayer", "Target player to change its role", ArgTypes.PLAYER_REF);
		RequiredArg<String> roleArg = this.withRequiredArg("role", "Target role to be set", ArgTypes.STRING);

		public SetRoleCommand() {
			super("set", "Sets a player role");
			requirePermission(TTT_ROLE_SET);
		}

		@Override
		protected void executeSync(@Nonnull CommandContext ctx) {
			String roleName = roleArg.get(ctx);
			var role = config.get().getRoleByName(roleName);
			if (role.isEmpty()) {
				ctx.sendMessage(Message.raw("Role {roleName} could not be found.")
						.param("roleName", roleName));
				return;
			}

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

				player.info().setCurrentRoundRole(role.get());
				player.info().setCredits(role.get().getStartingCredits());

				if (RoleGroup.TRAITOR.equals(role.get().getRoleGroup())) {
					gameModeState.traitorsAlive.add(player.refComponent().getUuid());

				} else if (RoleGroup.INNOCENT.equals(role.get().getRoleGroup())) {
					gameModeState.innocentsAlive.add(player.refComponent().getUuid());
				}

				player.info().getHud().update();

				//            if (FinishCurrentRoundEventHandler.roundShouldEnd(gameModeState)) {
				//                HytaleServer.get().getEventBus()
				//                        .dispatchForAsync(FinishCurrentRoundEvent.class)
				//                        .dispatch(new FinishCurrentRoundEvent(world.getWorldConfig().getUuid()));
				//            }
			});

		}
	}


}
