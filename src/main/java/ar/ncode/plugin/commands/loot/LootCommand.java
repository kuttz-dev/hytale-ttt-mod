package ar.ncode.plugin.commands.loot;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class LootCommand extends AbstractCommandCollection {

	public LootCommand() {
		super("loot", "Commands to manage loot in the world.");
		this.addSubCommand(new LootSpawnCommand());
	}

}
