package ar.ncode.plugin.commands;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.enums.PlayerRole;
import ar.ncode.plugin.ui.pages.ShopPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.model.CustomPermissions.TTT_SHOP_OPEN;
import static ar.ncode.plugin.model.MessageId.SHOP_ONLY_FOR_TRAITORS_OR_DETECTIVES;

public class Shop extends CommandBase {

	public static final List<PlayerRole> ROLES_WITH_SPECIAL_STORE = List.of(PlayerRole.TRAITOR, PlayerRole.DETECTIVE);

	public Shop() {
		super("shop", "Command to open a special store, available only for traitors or detectives");
		super.addAliases("store", "buy");
		requirePermission(TTT_SHOP_OPEN);
	}

	private static void openShopForPlayer(@NonNullDecl CommandContext ctx, Ref<EntityStore> reference) {
		var playerInfo = reference.getStore().getComponent(reference, PlayerGameModeInfo.componentType);
		var player = reference.getStore().getComponent(reference, Player.getComponentType());
		var playerRef = reference.getStore().getComponent(reference, PlayerRef.getComponentType());

		if (playerInfo == null || player == null || playerRef == null) {
			ctx.sendMessage(Message.raw("An error occurred while trying to access your component information."));
			return;
		}

		openShopIfUserHasRole(reference, playerRef, player, playerInfo);
	}

	public static void openShopIfUserHasRole(Ref<EntityStore> reference, PlayerRef playerRef, Player player, PlayerGameModeInfo playerInfo) {
		if (!ROLES_WITH_SPECIAL_STORE.contains(playerInfo.getRole())) {
			NotificationUtil.sendNotification(
					playerRef.getPacketHandler(),
					Message.translation(SHOP_ONLY_FOR_TRAITORS_OR_DETECTIVES.get()),
					NotificationStyle.Danger
			);
			return;
		}

		List<List<ItemStack>> items;
		if (PlayerRole.TRAITOR.equals(playerInfo.getRole())) {
			items = config.get().getItemsGroups(config.get().getTraitorStoreItems());
		} else {
			items = config.get().getItemsGroups(config.get().getDetectiveStoreItems());
		}

		player.getPageManager().openCustomPage(
				reference, reference.getStore(), new ShopPage(playerRef, CustomPageLifetime.CanDismiss, items, playerInfo)
		);
	}

	@Override
	protected void executeSync(@NonNullDecl CommandContext ctx) {
		Ref<EntityStore> reference = ctx.senderAsPlayerRef();

		if (reference == null || !reference.isValid()) {
			ctx.sendMessage(Message.raw("You can't use this command from the console."));
			return;
		}

		var world = reference.getStore().getExternalData().getWorld();
		world.execute(() -> openShopForPlayer(ctx, reference));
	}
}
