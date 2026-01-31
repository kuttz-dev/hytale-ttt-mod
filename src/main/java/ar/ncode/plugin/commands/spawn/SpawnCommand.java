package ar.ncode.plugin.commands.spawn;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class SpawnCommand extends AbstractCommandCollection {

	public SpawnCommand() {
		super("spawn", "Commands to manage loot in the world.");
		this.addSubCommand(new AddSpawnPointCommand());
		this.addSubCommand(new ShowSpawnPoints());
	}
}
