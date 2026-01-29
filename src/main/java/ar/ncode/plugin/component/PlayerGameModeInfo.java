package ar.ncode.plugin.component;

import ar.ncode.plugin.component.enums.PlayerRole;
import ar.ncode.plugin.model.DamageCause;
import ar.ncode.plugin.ui.hud.PlayerCurrentRoleHud;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameModeInfo implements Component<EntityStore> {

	public static final BuilderCodec<PlayerGameModeInfo> CODEC =
			BuilderCodec.builder(PlayerGameModeInfo.class, PlayerGameModeInfo::new)
					.append(new KeyedCodec<>("Karma", Codec.INTEGER),
							(c, v) -> c.karma = v, c -> c.karma)
					.add()
					.append(new KeyedCodec<>("TimeOfDeath", Codec.STRING),
							(c, v) -> c.timeOfDeath = v,
							c -> c.timeOfDeath)
					.add()
					.append(new KeyedCodec<>("CauseOfDeath", Codec.STRING),
							(c, v) -> c.causeOfDeath = "".equals(v) ? null : DamageCause.valueOf(v.toUpperCase()),
							c -> c.causeOfDeath == null ? "" : c.causeOfDeath.name()
					)
					.add()
					.append(new KeyedCodec<>("CurrentRoundRole", Codec.STRING),
							(c, v) -> c.currentRoundRole = "".equals(v) ? null : PlayerRole.valueOf(v), c -> c.currentRoundRole == null ? "" :
									c.currentRoundRole.name())
					.add()
					.build();

	public static ComponentType<EntityStore, PlayerGameModeInfo> componentType;

	private int karma = config.get().getKarmaStartingValue();
	private PlayerRole role = PlayerRole.SPECTATOR;
	private int kills = 0;
	private int deaths = 0;
	private int credits = 0;
	private PlayerCurrentRoleHud hud;
	private String timeOfDeath;
	private DamageCause causeOfDeath;
	private PlayerRole currentRoundRole;
	private float elapsedTimeSinceLastUpdate = 0;
	private boolean alreadyVotedMap = false;
	private String worldInstance = null;

	@NullableDecl
	@Override
	public Component<EntityStore> clone() {
		return new PlayerGameModeInfo(this.karma, this.role, this.kills, this.deaths, this.credits, this.hud,
				timeOfDeath, causeOfDeath, currentRoundRole, elapsedTimeSinceLastUpdate, alreadyVotedMap, worldInstance);
	}
}
