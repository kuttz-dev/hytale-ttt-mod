package ar.ncode.plugin.model.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RoleGroup {

	INNOCENT("#33CC76"),
	TRAITOR("#B01515"),
	;

	public final String guiColor;

}
