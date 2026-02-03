package ar.ncode.plugin.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DebugConfig {

	public static final DebugConfig INSTANCE = new DebugConfig();

	private boolean enableChangingGameMode = false;
	private boolean persistentGraveStones = false;
	private boolean canPlaceAndDestroyBlocks = false;

}
