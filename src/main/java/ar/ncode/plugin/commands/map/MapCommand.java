package ar.ncode.plugin.commands.map;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class MapCommand extends AbstractCommandCollection {

	public MapCommand() {
		super("map", "Commands related to map management");
		addSubCommand(new MapVoteCommand());
		addSubCommand(new FinishCurrentMapCommand());
		addSubCommand(new CrudMapCommand.CreateMapCommand());
		addSubCommand(new CrudMapCommand.ReadMapsCommand());
		addSubCommand(new CrudMapCommand.UpdateMapCommand());
		addSubCommand(new CrudMapCommand.DeleteMapCommand());
	}


}
