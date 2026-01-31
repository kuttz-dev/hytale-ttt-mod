package ar.ncode.plugin.ui.hud;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.enums.PlayerRole;
import ar.ncode.plugin.component.enums.RoundState;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.MessageId;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.time.LocalTime;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.GameModeState.timeFormatter;
import static ar.ncode.plugin.model.MessageId.HUD_CURRENT_ROLE_TRAITOR;

public class PlayerCurrentRoleHud extends CustomUIHud {

	public static final String PLAYER_CURRENT_ROLE_TEXT = "#PlayerCurrentRole.Text";
	public static final String PLAYER_CURRENT_ROLE_CONTAINER = "#PlayerCurrentRoleContainer";
	public static final String PLAYER_CURRENT_ROLE_BACKGROUND = PLAYER_CURRENT_ROLE_CONTAINER + ".Background";
	public static final String GRAY_BACKGROUND = "#838D9C";
	public static final String TRAITOR_COLOR = "#B01515";
	private final PlayerRef playerRef;
	private final PlayerGameModeInfo playerInfo;
	private MessageId messageId;
	private String backgroundColor;

	public PlayerCurrentRoleHud(@NonNullDecl PlayerRef playerRef, PlayerGameModeInfo playerInfo) {
		super(playerRef);
		this.playerRef = playerRef;
		this.playerInfo = playerInfo;
	}

	@Override
	protected void build(@NonNullDecl UICommandBuilder builder) {
		builder.append("Hud/hud.ui");
		setUpMessageAndBackgroundByPlayerRole(playerInfo.getRole());
		setUpHudValues(builder);
	}

	public void update() {
		GameModeState gameModeState = gameModeStateForWorld.get(playerRef.getWorldUuid());
		if (gameModeState == null) {
			return;
		}

		var builder = new UICommandBuilder();
		setUpMessageAndBackgroundByPlayerRole(playerInfo.getRole());
		setUpHudValues(builder);

		if (RoundState.IN_GAME.equals(gameModeState.roundState)) {
			LocalTime roundRemainingTime = gameModeState.getRoundRemainingTime();
			builder.set("#LeftRoundTime.Text", roundRemainingTime.format(timeFormatter));

		} else {
			builder.set("#LeftRoundTime.Text", "--" + ":" + "--");
		}

		update(false, builder);
	}


	private void setUpHudValues(UICommandBuilder builder) {
		builder.set(PLAYER_CURRENT_ROLE_TEXT, Message.translation(messageId.get()));
		builder.set(PLAYER_CURRENT_ROLE_BACKGROUND, backgroundColor);
	}

	private void setUpMessageAndBackgroundByPlayerRole(PlayerRole playerRole) {
		switch (playerRole) {
			case INNOCENT ->
					setUpMessageAndBackground(MessageId.HUD_CURRENT_ROLE_INNOCENT, TroubleInTrorkTownPlugin.config.get().getInnocentColor());
			case TRAITOR -> setUpMessageAndBackground(HUD_CURRENT_ROLE_TRAITOR, TRAITOR_COLOR);
			case DETECTIVE -> setUpMessageAndBackground(MessageId.HUD_CURRENT_ROLE_DETECTIVE, "#1F5CC4");
			case SPECTATOR -> setUpMessageAndBackground(MessageId.HUD_CURRENT_ROLE_SPECTATOR, GRAY_BACKGROUND);
			default -> setUpMessageAndBackground(MessageId.HUD_CURRENT_ROLE_PREPARING, GRAY_BACKGROUND);
		}
	}

	private void setUpMessageAndBackground(MessageId message, String color) {
		this.messageId = message;
		this.backgroundColor = color;
	}
}
