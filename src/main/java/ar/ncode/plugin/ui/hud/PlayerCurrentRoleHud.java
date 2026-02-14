package ar.ncode.plugin.ui.hud;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.GameModeState;
import ar.ncode.plugin.model.TranslationKey;
import ar.ncode.plugin.model.enums.RoundState;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.time.LocalTime;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.GameModeState.timeFormatter;

public class PlayerCurrentRoleHud extends CustomUIHud {

	public static final String PLAYER_CURRENT_ROLE_TEXT = "#PlayerCurrentRole.Text";
	public static final String PLAYER_CURRENT_ROLE_CONTAINER = "#PlayerCurrentRoleContainer";
	public static final String PLAYER_CURRENT_ROLE_BACKGROUND = PLAYER_CURRENT_ROLE_CONTAINER + ".Background";
	private final PlayerRef playerRef;
	private final PlayerGameModeInfo playerInfo;
	private String messageId;
	private String backgroundColor;

	public PlayerCurrentRoleHud(@NonNullDecl PlayerRef playerRef, PlayerGameModeInfo playerInfo) {
		super(playerRef);
		this.playerRef = playerRef;
		this.playerInfo = playerInfo;
	}

	@Override
	protected void build(@NonNullDecl UICommandBuilder builder) {
		GameModeState gameModeState = gameModeStateForWorld.get(playerRef.getWorldUuid());
		builder.append("Hud/hud.ui");
		setHudRoleValues(builder, gameModeState);
	}

	private void setHudRoleValues(@NonNullDecl UICommandBuilder builder, GameModeState gameModeState) {
		if (RoundState.PREPARING.equals(gameModeState.roundState)) {
			this.messageId = TranslationKey.HUD_CURRENT_STATUS_PREPARING.get();
			this.backgroundColor = TranslationKey.HUD_CURRENT_STATUS_PREPARING.getMessageColor();
		}

		if (playerInfo.getCurrentRoundRole() == null) {
			setHudRoleValues(builder);
			return;
		}

		if (playerInfo.isSpectator()) {
			this.messageId = TranslationKey.HUD_CURRENT_STATUS_SPECTATOR.get();
			this.backgroundColor = TranslationKey.HUD_CURRENT_STATUS_SPECTATOR.getMessageColor();

		} else {
			this.messageId = playerInfo.getCurrentRoundRole().getTranslationKey();

			if (playerInfo.getCurrentRoundRole().getCustomGuiColor() != null) {
				this.backgroundColor = playerInfo.getCurrentRoundRole().getCustomGuiColor();

			} else {
				this.backgroundColor = playerInfo.getCurrentRoundRole().getRoleGroup().guiColor;

			}
		}

		setHudRoleValues(builder);
	}

	private void setHudRoleValues(@NonNullDecl UICommandBuilder builder) {
		if (messageId != null && backgroundColor != null) {
			builder.set(PLAYER_CURRENT_ROLE_TEXT, Message.translation(messageId));
			builder.set(PLAYER_CURRENT_ROLE_BACKGROUND, backgroundColor);
		}
	}

	public void update() {
		GameModeState gameModeState = gameModeStateForWorld.get(playerRef.getWorldUuid());
		if (gameModeState == null) {
			return;
		}

		var builder = new UICommandBuilder();
		setHudRoleValues(builder, gameModeState);

		if (RoundState.IN_GAME.equals(gameModeState.roundState)) {
			LocalTime roundRemainingTime = gameModeState.getRoundRemainingTime();
			builder.set("#LeftRoundTime.Text", roundRemainingTime.format(timeFormatter));

		} else {
			builder.set("#LeftRoundTime.Text", "--" + ":" + "--");
		}

		update(false, builder);
	}

}
