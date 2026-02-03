package ar.ncode.plugin.commands.map;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class MapCommand extends AbstractCommandCollection {

	public MapCommand() {
		super("map", "description");
		addSubCommand(new MapVoteCommand());
		addSubCommand(new FinishCurrentMapCommand());
	}


}
