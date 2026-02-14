package ar.ncode.plugin.model;

import static ar.ncode.plugin.model.TranslationKey.LANG_FILE_NAME;

public enum DamageCause {

	PHYSICAL,
	PROJECTILE,
	COMMAND,
	DROWNING,
	ENVIRONMENT,
	FALL,
	OUT_OF_WORLD,
	OUTOFWORLD,
	SUFFOCATION,
	FIRE;

	public String getTranslationKey() {
		return LANG_FILE_NAME + "." + "dead_reason_" + this.name().toLowerCase();
	}


}
