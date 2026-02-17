package ar.ncode.plugin.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
public class DebugConfig {

	public static final DebugConfig INSTANCE = new DebugConfig();

	private boolean enableChangingGameMode = false;
	private boolean persistentGraveStones = false;
	private boolean canPlaceAndDestroyBlocks = false;
	@Accessors(fluent = true)
	private boolean entitiesShouldDisappearAfterRound = true;

	public static DebugConfig get() {
		return INSTANCE;
	}

}
