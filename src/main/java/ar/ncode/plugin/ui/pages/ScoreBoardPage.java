package ar.ncode.plugin.ui.pages;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.component.enums.PlayerRole;
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
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.component.enums.PlayerRole.SPECTATOR;
import static ar.ncode.plugin.component.enums.PlayerRole.TRAITOR;
import static ar.ncode.plugin.model.MessageId.*;

public class ScoreBoardPage extends BasicCustomUIPage {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private int rowNumber = 1;

	public ScoreBoardPage(@NonNullDecl PlayerRef playerRef, @NonNullDecl CustomPageLifetime lifetime) {
		super(playerRef, lifetime);
	}

	@NonNullDecl
	public static Message getRoleTranslation(PlayerRole role) {
		return switch (role) {
			case PREPARING -> Message.translation(HUD_CURRENT_ROLE_PREPARING.get());
			case INNOCENT -> Message.translation(HUD_CURRENT_ROLE_INNOCENT.get());
			case DETECTIVE -> Message.translation(HUD_CURRENT_ROLE_DETECTIVE.get());
			case TRAITOR -> Message.translation(HUD_CURRENT_ROLE_TRAITOR.get());
			case SPECTATOR -> Message.translation(HUD_CURRENT_ROLE_SPECTATOR.get());
		};
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
		builder.set("#titleRole.Text", Message.translation(SCOREBOARD_TITLES_ROLE.get()));
		builder.set("#titleKarma.Text", Message.translation(SCOREBOARD_TITLES_KARMA.get()));
		builder.set("#titleKills.Text", Message.translation(SCOREBOARD_TITLES_KILLS.get()));
		builder.set("#titleDeaths.Text", Message.translation(SCOREBOARD_TITLES_DEATHS.get()));
		builder.set("#titlePing.Text", Message.translation(SCOREBOARD_TITLES_PING.get()));
	}

	private void buildRow(@NonNullDecl UICommandBuilder builder, PlayerRef targetPlayerRef, PlayerGameModeInfo targetPlayerInfo, boolean showTraitors) {
		builder.append("#Content", "Pages/Scoreboard/scoreboard-row.ui");
		String rowPrefix = "#Content[" + rowNumber + "]";

		PlayerRole targetPlayerRole = targetPlayerInfo.getCurrentRoundRole() == null ?
				targetPlayerInfo.getRole() : targetPlayerInfo.getCurrentRoundRole();

		if (showTraitors && TRAITOR.equals(targetPlayerRole)) {
			String backgroundColor = config.get().getTraitorColor();
			builder.set(rowPrefix + ".Background", backgroundColor);
		}

		builder.set(rowPrefix + " #rowPlayerName.Text", targetPlayerRef.getUsername());
		builder.set(rowPrefix + " #rowRole.Text", getRoleTranslation(targetPlayerRole));
		builder.set(rowPrefix + " #rowKarma.Text", String.valueOf(targetPlayerInfo.getKarma()));

		int ping = getAvgPing(targetPlayerRef);
		builder.set(rowPrefix + " #rowPing.Text", String.valueOf(ping));
	}

	private void buildTableRecords(@NonNullDecl UICommandBuilder builder, List<TableRecord> confirmedDeaths, boolean showTraitors) {
		for (TableRecord record : confirmedDeaths) {
			buildRow(builder, record.playerRef, record.playerInfo, showTraitors);
			rowNumber++;
		}
	}

	@Override
	public void build(@NonNullDecl UICommandBuilder builder) {
		buildBaseScoreBoard(builder);

		Collection<PlayerRef> players = getPlayerRefs();
		if (players == null) return;

		addRowForEachPlayerToScoreBoard(builder, players);

		LOGGER.at(Level.FINER).log("Custom scoreboard build");
	}

	@NullableDecl
	private Collection<PlayerRef> getPlayerRefs() {
		if (super.playerRef.getWorldUuid() == null) {
			return null;
		}

		World world = Universe.get().getWorld(super.playerRef.getWorldUuid());
		if (world == null) {
			return null;
		}

		return world.getPlayerRefs();
	}

	private void addRowForEachPlayerToScoreBoard(@NonNullDecl UICommandBuilder builder, Collection<PlayerRef> players) {
		Ref<EntityStore> reference = playerRef.getReference();
		if (reference == null || !reference.isValid()) {
			return;
		}

		PlayerGameModeInfo playerInfo = reference.getStore().getComponent(reference, PlayerGameModeInfo.componentType);
		if (playerInfo == null) {
			return;
		}

		boolean showTraitors = TRAITOR.equals(playerInfo.getCurrentRoundRole());
		List<TableRecord> lostsInCombat = new ArrayList<>();
		List<TableRecord> confirmedDeaths = new ArrayList<>();
		List<TableRecord> trueSpectators = new ArrayList<>();

		for (PlayerRef targetPlayerRef : players) {
			Ref<EntityStore> targetReference = targetPlayerRef.getReference();
			PlayerGameModeInfo targetPlayerInfo = targetReference.getStore().getComponent(targetReference,
					PlayerGameModeInfo.componentType);

			if (!targetReference.isValid() || targetPlayerInfo == null) {
				continue;
			}

			ConfirmedDeath confirmedDeath = targetReference.getStore().getComponent(targetReference,
					ConfirmedDeath.componentType);

			if (confirmedDeath != null) {
				confirmedDeaths.add(new TableRecord(targetPlayerRef, targetPlayerInfo));
				continue;
			}

			LostInCombat lostInCombat = targetReference.getStore().getComponent(targetReference,
					LostInCombat.componentType);

			if (lostInCombat != null && SPECTATOR.equals(playerInfo.getRole())) {
				lostsInCombat.add(new TableRecord(targetPlayerRef, targetPlayerInfo));
				continue;
			}

			if (SPECTATOR.equals(targetPlayerInfo.getRole())) {
				trueSpectators.add(new TableRecord(targetPlayerRef, targetPlayerInfo));
				continue;
			}

			buildRow(builder, targetPlayerRef, targetPlayerInfo, showTraitors);
			rowNumber++;
		}

		if (SPECTATOR.equals(playerInfo.getRole())) {
			builder.append("#Content", "Pages/Scoreboard/scoreboard-lost-in-combat.ui");
			rowNumber++;

			buildTableRecords(builder, lostsInCombat, showTraitors);
		}

		if (!confirmedDeaths.isEmpty()) {
			builder.append("#Content", "Pages/Scoreboard/scoreboard-confirmed-death.ui");
			rowNumber++;

			buildTableRecords(builder, confirmedDeaths, true);
		}

		if (!trueSpectators.isEmpty()) {
			builder.append("#Content", "Pages/Scoreboard/scoreboard-spectators.ui");
			rowNumber++;

			buildTableRecords(builder, trueSpectators, showTraitors);
		}
	}

	@RequiredArgsConstructor
	private static class TableRecord {
		private final PlayerRef playerRef;
		private final PlayerGameModeInfo playerInfo;
	}

}
