package ar.ncode.plugin.config.instance.loot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter()
@Setter()
@NoArgsConstructor()
public class LootItem {

	public static final BuilderCodec<LootItem> CODEC =
			BuilderCodec.builder(LootItem.class, LootItem::new)
					.append(new KeyedCodec<>("Probability", Codec.INTEGER),
							(c, value, extraInfo) -> c.probability = value,
							(c, extraInfo) -> c.probability)
					.add()
					.append(new KeyedCodec<>("Amount", Codec.INTEGER),
							(c, value, extraInfo) -> c.amount = value,
							(c, extraInfo) -> c.amount)
					.add()
					.append(new KeyedCodec<>("ItemId", Codec.STRING),
							(c, value, extraInfo) -> c.itemId = value,
							(c, extraInfo) -> c.itemId)
					.add()
					.append(new KeyedCodec<>("Includes", ArrayCodec.ofBuilderCodec(IncludedLootItem.CODEC, IncludedLootItem[]::new)),
							(c, value, extraInfo) -> c.includes = value,
							(c, extraInfo) -> c.includes)
					.add()
					.build();

	private int probability = 100;
	private int amount = 1;
	private String itemId;
	private IncludedLootItem[] includes = new IncludedLootItem[]{};


}
