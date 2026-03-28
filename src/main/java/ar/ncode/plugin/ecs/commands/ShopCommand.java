package ar.ncode.plugin.ecs.commands;

import ar.ncode.plugin.accessors.PlayerAccessors;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoundState;
import ar.ncode.plugin.ui.pages.ShopPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.getCurrentGameModeStateForCurrentWorld;
import static ar.ncode.plugin.model.CustomPermissions.TTT_SHOP_OPEN;
import static ar.ncode.plugin.model.enums.TranslationKey.SHOP_CAN_ONLY_BE_OPENED_DURING_ROUND;
import static ar.ncode.plugin.model.enums.TranslationKey.SHOP_ONLY_FOR_TRAITORS_OR_DETECTIVES;

public class ShopCommand extends CommandBase {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ShopCommand() {
        super("shop", "Command to open a special store, available only for traitors or detectives");
        super.addAliases("store", "buy");
        requirePermission(TTT_SHOP_OPEN);
    }

    private static void openShopForPlayer(@NonNullDecl CommandContext ctx, Ref<EntityStore> reference) {
        var player = PlayerAccessors.getPlayerFrom(reference, reference.getStore());

        if (player.isEmpty()) {
            ctx.sendMessage(Message.raw("An error occurred while trying to access your component information."));
            return;
        }

        openShopForPlayer(reference, player.get());
    }


    public static void openShopForPlayer(Ref<EntityStore> reference, PlayerComponents player) {
        var currentState = getCurrentGameModeStateForCurrentWorld();
        if (currentState == null) {
            LOGGER.atInfo().log("An error occurred while trying to access the game mode state.");
            return;
        }

        if (!RoundState.IN_GAME.equals(currentState.getRoundState())) {
            NotificationUtil.sendNotification(
                    player.refComponent().getPacketHandler(),
                    Message.translation(SHOP_CAN_ONLY_BE_OPENED_DURING_ROUND.get()),
                    NotificationStyle.Danger
            );
            return;
        }

        if (player.info().getCurrentRoundRole() == null || !player.info().getCurrentRoundRole().hasStore()) {
            NotificationUtil.sendNotification(
                    player.refComponent().getPacketHandler(),
                    Message.translation(SHOP_ONLY_FOR_TRAITORS_OR_DETECTIVES.get()),
                    NotificationStyle.Danger
            );
            return;
        }

        List<List<ItemStack>> items;
        items = config.get().getItemsGroups(player.info().getCurrentRoundRole().getStoreItems());

        player.component().getPageManager().openCustomPage(
                reference, reference.getStore(), new ShopPage(player.refComponent(), CustomPageLifetime.CanDismiss,
                        items, player.info())
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
