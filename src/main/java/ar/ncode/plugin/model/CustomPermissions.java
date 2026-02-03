package ar.ncode.plugin.model;

import java.util.Set;

public class CustomPermissions {

	public static final String TTT_PREFIX = "ttt";
	public static final String TTT_MAP_VOTE = TTT_PREFIX + ".map.vote";
	public static final String TTT_SHOP_OPEN = TTT_PREFIX + ".shop.open";
	public static final Set<String> USER_PERMISSIONS = Set.of(
			TTT_MAP_VOTE,
			TTT_SHOP_OPEN
	);
	public static final String TTT_USER_GROUP = TTT_PREFIX + ".groups.user";
}
