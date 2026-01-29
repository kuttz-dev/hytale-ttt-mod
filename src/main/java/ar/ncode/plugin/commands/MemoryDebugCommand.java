package ar.ncode.plugin.commands;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.system.scheduled.DoubleTapDetector;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Debug command to force garbage collection and display memory statistics.
 * Useful for diagnosing memory leaks vs lazy GC behavior.
 */
public class MemoryDebugCommand extends CommandBase {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	public MemoryDebugCommand() {
		super("ttt-memory", "Force GC and display memory statistics for debugging.");
	}

	@Override
	protected void executeSync(@NonNullDecl CommandContext commandContext) {
		Runtime rt = Runtime.getRuntime();

		// Memory before GC
		long usedBefore = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		long totalBefore = rt.totalMemory() / 1024 / 1024;

		commandContext.sendMessage(Message.raw("§e=== Memory Debug ==="));
		commandContext.sendMessage(Message.raw("Before GC: " + usedBefore + "MB / " + totalBefore + "MB"));

		// Force garbage collection
		System.gc();

		// Small delay to let GC complete
		try {
			Thread.sleep(100);
		} catch (InterruptedException ignored) {
		}

		// Memory after GC
		long usedAfter = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		long totalAfter = rt.totalMemory() / 1024 / 1024;
		long freed = usedBefore - usedAfter;

		commandContext.sendMessage(Message.raw("After GC: " + usedAfter + "MB / " + totalAfter + "MB"));
		commandContext.sendMessage(Message.raw("Freed:    " + freed + "MB"));

		// Additional debug info
		int worldCount = Universe.get().getWorlds().size();
		int gameStateCount = TroubleInTrorkTownPlugin.gameModeStateForWorld.size();
		int playerStateCount = DoubleTapDetector.getInstance().getPlayerStateCount();

		commandContext.sendMessage(Message.raw("=== TTT State ==="));
		commandContext.sendMessage(Message.raw("Worlds in Universe:     " + worldCount));
		commandContext.sendMessage(Message.raw("GameModeStates:         " + gameStateCount));
		commandContext.sendMessage(Message.raw("DoubleTap PlayerStates: " + playerStateCount));

		// Log to console as well
		LOGGER.atInfo().log("Memory Debug - Before: %dMB, After: %dMB, Freed: %dMB, Worlds: %d, GameStates: %d, PlayerStates: %d",
				usedBefore, usedAfter, freed, worldCount, gameStateCount, playerStateCount);
	}
}
