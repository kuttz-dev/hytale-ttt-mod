package ar.ncode.plugin.config.loot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter()
@Setter()
@NoArgsConstructor()
public class IncludedLootItem {

	public static final BuilderCodec<IncludedLootItem> CODEC =
			BuilderCodec.builder(IncludedLootItem.class, IncludedLootItem::new)
					.append(new KeyedCodec<>("Amount", Codec.INTEGER),
							(c, value, extraInfo) -> c.amount = value,
							(c, extraInfo) -> c.amount)
					.add()
					.append(new KeyedCodec<>("ItemId", Codec.STRING),
							(c, value, extraInfo) -> c.itemId = value,
							(c, extraInfo) -> c.itemId)
					.add()
					.build();

	private int amount = 1;
	private String itemId;

}
