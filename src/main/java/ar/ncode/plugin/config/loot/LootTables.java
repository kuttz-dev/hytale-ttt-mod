package ar.ncode.plugin.config.loot;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter()
@Setter()
@NoArgsConstructor()
public class LootTables {

	public static final BuilderCodec<LootTables> CODEC = BuilderCodec.builder(LootTables.class, LootTables::new)
			.append(new KeyedCodec<>("LootTables", ArrayCodec.ofBuilderCodec(LootTable.CODEC, LootTable[]::new)),
					(c, value, extraInfo) -> c.lootTables = value,
					(c, extraInfo) -> c.lootTables)
			.add()
			.build();


	private LootTable[] lootTables = new LootTable[]{};


	public LootTable getLootTableById(String id) {
		for (LootTable lootTable : lootTables) {
			if (lootTable.getId().equalsIgnoreCase(id)) {
				return lootTable;
			}
		}

		return null;
	}

}
