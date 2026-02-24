package ar.ncode.plugin.config.loot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

@Getter()
@Setter()
@NoArgsConstructor()
public class LootTable {

	public static final BuilderCodec<LootTable> CODEC = BuilderCodec.builder(LootTable.class, LootTable::new)
			.append(new KeyedCodec<>("Id", Codec.STRING),
					(c, value, extraInfo) -> c.id = value,
					(c, extraInfo) -> c.id)
			.add()
			.append(new KeyedCodec<>("Items", ArrayCodec.ofBuilderCodec(LootItem.CODEC, LootItem[]::new)),
					(c, value, extraInfo) -> c.items = value,
					(c, extraInfo) -> c.items)
			.add()
			.append(new KeyedCodec<>("MaxItems", Codec.INTEGER),
					(c, value, extraInfo) -> c.maxItems = value,
					(c, extraInfo) -> c.maxItems)
			.add()
			.build();


	private String id;
	private LootItem[] items = new LootItem[]{};
	private Integer maxItems;

	@Override
	public String toString() {
		return "LootBox{" +
				"possibleLoot=" + Arrays.toString(items) +
				'}';
	}
}
