package ar.ncode.plugin.ui.pages;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.model.PlayerComponents;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.protocol.packets.connection.PongType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import static ar.ncode.plugin.accessors.WorldAccessors.getPlayersAt;
import static ar.ncode.plugin.model.TranslationKey.*;
import static ar.ncode.plugin.model.enums.RoleGroup.TRAITOR;

public class ScoreBoardPage extends BasicCustomUIPage {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private int rowNumber = 1;

	public ScoreBoardPage(@NonNullDecl PlayerRef playerRef, @NonNullDecl CustomPageLifetime lifetime) {
		super(playerRef, lifetime);
	}

	private static int getAvgPing(PlayerRef playerRef) {
		PacketHandler.PingInfo pingInfo = playerRef.getPacketHandler().getPingInfo(PongType.Direct);
		double average = pingInfo.getPingMetricSet().getAverage(0);
		return (int) PacketHandler.PingInfo.TIME_UNIT.toMillis(MathUtil.fastCeil(average));
	}

	private static void buildBaseScoreBoard(@NonNullDecl UICommandBuilder builder) {
		builder.append("Pages/Scoreboard/scoreboard.ui");
		builder.set("#ScoreBoardTitle.Text", Message.translation(SCOREBOARD_TITLE.get()));
		builder.set("#titlePlayer.Text", Message.translation(SCOREBOARD_TITLES_PLAYER.get()));
		builder.set("#titleKarma.Text", Message.translation(SCOREBOARD_TITLES_KARMA.get()));
		builder.set("#titleKills.Text", Message.translation(SCOREBOARD_TITLES_KILLS.get()));
		builder.set("#titleDeaths.Text", Message.translation(SCOREBOARD_TITLES_DEATHS.get()));
		builder.set("#titlePing.Text", Message.translation(SCOREBOARD_TITLES_PING.get()));
	}

	public static ScoreBoardTable getTableRows(Collection<PlayerComponents> players, boolean showLostInCombat) {
		ScoreBoardTable result = new ScoreBoardTable();
		for (var player : players) {
			Ref<EntityStore> reference = player.reference();
			if (!reference.isValid() || player.info() == null) {
				continue;
			}

			boolean confirmedDeath = reference.getStore().getComponent(reference, ConfirmedDeath.componentType) != null;
			if (confirmedDeath) {
				result.confirmedDeaths.add(player);
				continue;
			}

			boolean lostInCombat = reference.getStore().getComponent(reference, LostInCombat.componentType) != null;

			if (lostInCombat && showLostInCombat) {
				result.lostInCombat.add(player);
				continue;
			}

			if (player.info().getCurrentRoundRole() == null) {
				result.spectators.add(player);
				continue;
			}

			result.alivePlayers.add(player);
		}

		return result;
	}

	private void buildTableRecords(@NonNullDecl UICommandBuilder builder, List<PlayerComponents> group, boolean showTraitors) {
		for (var player : group) {
			builder.append("#Content", "Pages/Scoreboard/scoreboard-row.ui");
			String rowPrefix = "#Content[" + rowNumber + "]";

			CustomRole targetRoleGroup = player.info().getCurrentRoundRole();

			if (targetRoleGroup != null && showTraitors && TRAITOR.equals(targetRoleGroup.getRoleGroup())) {
				builder.set(rowPrefix + ".Background", TRAITOR.guiColor);

			} else if (targetRoleGroup != null && !targetRoleGroup.isSecretRole()) {
				builder.set(rowPrefix + ".Background", targetRoleGroup.getCustomGuiColor());
			}

			builder.set(rowPrefix + " #rowPlayerName.Text", player.component().getDisplayName());
			builder.set(rowPrefix + " #rowKarma.Text", String.valueOf(player.info().getKarma()));

			int ping = getAvgPing(player.refComponent());
			builder.set(rowPrefix + " #rowPing.Text", String.valueOf(ping));

			rowNumber++;
		}
	}

	@Override
	public void build(@NonNullDecl UICommandBuilder builder) {
		buildBaseScoreBoard(builder);

		if (super.playerRef.getWorldUuid() == null) {
			return;
		}

		World world = Universe.get().getWorld(super.playerRef.getWorldUuid());
		if (world == null) return;

		addRowForEachPlayerToScoreBoard(builder, getPlayersAt(world));

		LOGGER.at(Level.FINER).log("Custom scoreboard build");
	}

	private void addRowForEachPlayerToScoreBoard(@NonNullDecl UICommandBuilder builder, Collection<PlayerComponents> players) {
		Ref<EntityStore> reference = playerRef.getReference();
		if (reference == null || !reference.isValid()) {
			return;
		}

		PlayerGameModeInfo playerInfo = reference.getStore().getComponent(reference, PlayerGameModeInfo.componentType);
		if (playerInfo == null) {
			return;
		}

		boolean showTraitors = TRAITOR.equals(playerInfo.getCurrentRoundRole().getRoleGroup());
		boolean showLostInCombat = playerInfo.isSpectator();
		ScoreBoardTable table = getTableRows(players, showLostInCombat);

		buildTableRecords(builder, table.alivePlayers, showTraitors);

		if (showLostInCombat) {
			builder.append("#Content", "Pages/Scoreboard/scoreboard-lost-in-combat.ui");
			rowNumber++;

			buildTableRecords(builder, table.lostInCombat, showTraitors);
		}

		if (!table.confirmedDeaths.isEmpty()) {
			builder.append("#Content", "Pages/Scoreboard/scoreboard-confirmed-death.ui");
			rowNumber++;

			buildTableRecords(builder, table.confirmedDeaths, true);
		}

		if (!table.spectators.isEmpty()) {
			builder.append("#Content", "Pages/Scoreboard/scoreboard-spectators.ui");
			rowNumber++;

			buildTableRecords(builder, table.spectators, showTraitors);
		}
	}

	public static class ScoreBoardTable {
		public final List<PlayerComponents> alivePlayers = new ArrayList<>();
		public final List<PlayerComponents> lostInCombat = new ArrayList<>();
		public final List<PlayerComponents> confirmedDeaths = new ArrayList<>();
		public final List<PlayerComponents> spectators = new ArrayList<>();
	}

}
