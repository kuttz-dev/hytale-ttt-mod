package ar.ncode.plugin.model;

import java.util.Set;

public class CustomPermissions {

	public static final String TTT_PREFIX = "ttt";
	public static final String TTT_MAP_VOTE = TTT_PREFIX + ".map.vote";
	public static final String TTT_SHOP_OPEN = TTT_PREFIX + ".shop.open";
	public static final String TTT_ROLE_SET = TTT_PREFIX + ".role.set";
	public static final String TTT_CREDITS_SET = TTT_PREFIX + ".credits.set";
	public static final Set<String> USER_PERMISSIONS = Set.of(
			TTT_MAP_VOTE,
			TTT_SHOP_OPEN
	);

	public static final Set<String> ADMIN_PERMISSIONS = Set.of(
			TTT_MAP_VOTE,
			TTT_SHOP_OPEN,
			TTT_CREDITS_SET,
			TTT_ROLE_SET
	);
	public static final String TTT_USER_GROUP = TTT_PREFIX + ".groups.user";
	public static final String TTT_ADMIN_GROUP = TTT_PREFIX + ".groups.admin";
}
