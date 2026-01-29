package ar.ncode.plugin.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;

public class Debug extends CommandBase {

	public Debug() {
		super("ttt-debug", "Command to toggle the debug mode.");
	}

	@Override
	protected void executeSync(@NonNullDecl CommandContext ctx) {
		config.get().setDebugMode(config.get().isDebugMode());
	}
	
}
