package ar.ncode.plugin.component;

import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.model.DamageCause;
import ar.ncode.plugin.ui.hud.PlayerCurrentRoleHud;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.UUID;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;

@Getter
@Setter
@ToString
@Builder
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
					.append(new KeyedCodec<>("CurrentRoundRole", CustomRole.CODEC),
							(c, v) -> c.currentRoundRole = v,
							c -> c.currentRoundRole
					)
					.add()
					.build();

	public static ComponentType<EntityStore, PlayerGameModeInfo> componentType;

	@Builder.Default
	private int karma = config.get().getKarmaStartingValue();
	@Builder.Default
	private int kills = 0;
	@Builder.Default
	private int deaths = 0;
	@Builder.Default
	private int credits = 0;
	private PlayerCurrentRoleHud hud;
	private String timeOfDeath;
	private DamageCause causeOfDeath;
	private CustomRole currentRoundRole;
	private boolean spectator;
	@Builder.Default
	private float elapsedTimeSinceLastUpdate = 0;
	@Builder.Default
	@Accessors(fluent = true)
	private boolean hasAlreadyVotedMap = false;
	@Builder.Default
	private UUID worldInstance = null;

	@NullableDecl
	@Override
	public Component<EntityStore> clone() {
		return new PlayerGameModeInfo(this.karma, this.kills, this.deaths, this.credits, this.hud,
				timeOfDeath, causeOfDeath, currentRoundRole, spectator, elapsedTimeSinceLastUpdate, hasAlreadyVotedMap,
				worldInstance);
	}

	public void incrementDeaths() {
		this.deaths++;
	}
}
