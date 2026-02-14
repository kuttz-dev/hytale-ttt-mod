package ar.ncode.plugin.ui.pages;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.StringUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.model.TranslationKey.*;

public class ShopPage extends InteractiveCustomUIPage<ShopPage.ShopInteractionEvent> {

	private final List<List<ItemStack>> items;
	private final PlayerGameModeInfo playerInfo;

	public ShopPage(@NonNullDecl PlayerRef playerRef, @NonNullDecl CustomPageLifetime lifetime, List<List<ItemStack>> items, PlayerGameModeInfo playerInfo) {
		super(playerRef, lifetime, ShopPage.ShopInteractionEvent.CODEC);
		this.items = items;
		this.playerInfo = playerInfo;
	}

	@Override
	public void build(
			@NonNullDecl Ref<EntityStore> reference, @NonNullDecl UICommandBuilder builder,
			@NonNullDecl UIEventBuilder eventBuilder, @NonNullDecl Store<EntityStore> store
	) {
		builder.append("Pages/Store/store.ui");
		builder.set("#TitleText.Text", Message.translation(SHOP_TITLE.get()));
		builder.set("#Credits.Text", Message.translation(SHOP_CREDITS.get()));
		builder.set("#CreditAmount.Text", String.valueOf(playerInfo.getCredits()));
		builder.set("#CancelTextButton.Text", Message.translation(SHOP_CLOSE.get()));

		int rowSize = config.get().getItemsInARowForTheShop();
		int rows = items.size() / rowSize;
		if (items.size() % rowSize > 0) {
			rows++;
		}

		builder.appendInline("#Rows", "Group #Row {}");

		for (int row = 0; row < rows; row++) {
			builder.appendInline("#Rows", "Group #Row { LayoutMode: Left; }");
			String rowSelector = "#Rows[" + (row + 1) + "]";
			builder.appendInline(rowSelector, "Group #Item {}");

			for (int i = 0; i < rowSize && (i + row * rowSize) < items.size(); i++) {
				int itemGroupIndex = i + row * rowSize;
				ItemStack itemStack = items.get(itemGroupIndex).getFirst();
				String itemId = itemStack.getItemId();
				var itemAsset = Item.getAssetMap().getAsset(itemId);

				if (itemAsset == null) {
					continue;
				}

				builder.append(rowSelector, "Pages/Store/store-item.ui");
				String itemPrefix = rowSelector + "[" + (i + 1) + "] ";
				builder.set(itemPrefix + "#ItemIcon.AssetPath", itemAsset.getIcon());
				builder.set(itemPrefix + "#ItemName.Text", Message.translation(itemAsset.getTranslationKey()));
				builder.set(itemPrefix + "#BuyButton.Text", Message.translation(SHOP_BUY.get()));
				eventBuilder.addEventBinding(
						CustomUIEventBindingType.Activating,
						itemPrefix + "#BuyButton",
						EventData.of("Item", String.valueOf(itemGroupIndex))
								.append("Amount", String.valueOf(itemStack.getQuantity()))
				);
			}

		}


		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelTextButton", EventData.of("Amount",
				String.valueOf(0)));
	}

	@Override
	public void handleDataEvent(
			Ref<EntityStore> reference, Store<EntityStore> store, ShopInteractionEvent event
	) {
		if (event == null || event.item == null) {
			close();
			return;
		}

		if (playerInfo.getCredits() <= 0) {
			NotificationUtil.sendNotification(
					playerRef.getPacketHandler(),
					Message.translation(SHOP_NOT_ENOUGH_CREDITS.get()),
					NotificationStyle.Danger
			);
			sendUpdate();
			return;
		}

		store.getExternalData().getWorld().execute(() -> {
			Player player = store.getComponent(reference, Player.getComponentType());

			if (player == null || event == null) {
				return;
			}

			playerInfo.setCredits(playerInfo.getCredits() - 1);
			int itemGroupIndex = StringUtil.isNumericString(event.item) ? Integer.parseInt(event.item) : 1;
			List<ItemStack> itemGroup = items.get(itemGroupIndex);

			for (ItemStack item : itemGroup) {
				player.getInventory().getCombinedHotbarFirst().addItemStack(item);
			}

			UICommandBuilder builder = new UICommandBuilder();
			builder.set("#CreditAmount.Text", String.valueOf(playerInfo.getCredits()));
			sendUpdate(builder);
		});

	}

	public static class ShopInteractionEvent {

		public static final BuilderCodec<ShopInteractionEvent> CODEC =
				BuilderCodec.builder(ShopInteractionEvent.class, ShopInteractionEvent::new)
						.append(new KeyedCodec<>("Item", Codec.STRING),
								(d, v) -> d.item = v, d -> d.item)
						.add()
						.append(new KeyedCodec<>("Amount", Codec.STRING),
								(d, v) -> d.amount = v, d -> d.amount)
						.add()
						.build();

		public String item;
		public String amount;
	}

}
