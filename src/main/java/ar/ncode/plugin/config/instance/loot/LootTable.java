package ar.ncode.plugin.config.instance.loot;

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
			.build();


	private String id;
	private LootItem[] items = new LootItem[]{};

	@Override
	public String toString() {
		return "LootBox{" +
				"possibleLoot=" + Arrays.toString(items) +
				'}';
	}
}
