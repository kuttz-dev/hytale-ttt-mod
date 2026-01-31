package ar.ncode.plugin.commands;

import ar.ncode.plugin.commands.debug.Debug;
import ar.ncode.plugin.commands.loot.LootCommand;
import ar.ncode.plugin.commands.map.MapCommand;
import ar.ncode.plugin.commands.spawn.SpawnCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class TttCommand extends AbstractCommandCollection {

	public TttCommand() {
		super("ttt", "Gamemode specific commands for Trouble in Trork Town.");
		this.addSubCommand(new Debug());
		this.addSubCommand(new LootCommand());
		this.addSubCommand(new SpawnCommand());
		this.addSubCommand(new MapCommand());
		this.addSubCommand(new Shop());
	}
}
