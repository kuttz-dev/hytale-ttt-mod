package ar.ncode.plugin.commands;

import ar.ncode.plugin.commands.debug.Debug;
import ar.ncode.plugin.commands.loot.LootCommand;
import ar.ncode.plugin.commands.map.MapCommand;
import ar.ncode.plugin.commands.spawn.SpawnCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import static ar.ncode.plugin.model.CustomPermissions.TTT_USER_GROUP;

public class TttCommand extends AbstractCommandCollection {

	public TttCommand() {
		super("ttt", "Gamemode specific commands for Trouble in Trork Town.");
		setPermissionGroups(TTT_USER_GROUP);

		this.addSubCommand(new Debug());
		this.addSubCommand(new LootCommand());
		this.addSubCommand(new SpawnCommand());
		this.addSubCommand(new MapCommand());
		this.addSubCommand(new Shop());
		this.addSubCommand(new RoleCommand());
		this.addSubCommand(new CreditsCommand());
		this.addSubCommand(new PlayerInfoCommand());
	}
}
