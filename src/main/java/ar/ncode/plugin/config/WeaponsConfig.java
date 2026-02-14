package ar.ncode.plugin.config;

import ar.ncode.plugin.config.loot.LootTable;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.Arrays;
import java.util.Optional;

import static com.hypixel.hytale.common.util.ArrayUtil.contains;

public class WeaponsConfig {

	public static final BuilderCodec<WeaponsConfig> CODEC =
			BuilderCodec.builder(WeaponsConfig.class, WeaponsConfig::new)
					.append(new KeyedCodec<>("WeaponTypes", ArrayCodec.ofBuilderCodec(WeaponTypeConfig.CODEC,
									WeaponTypeConfig[]::new)),
							(c, value, extraInfo) -> c.typesConfigs = value,
							(c, extraInfo) -> c.typesConfigs)
					.add()
					.append(new KeyedCodec<>("LootTables", ArrayCodec.ofBuilderCodec(LootTable.CODEC, LootTable[]::new)),
							(c, value, extraInfo) -> c.lootTables = value,
							(c, extraInfo) -> c.lootTables)
					.add()
					.build();

	private WeaponTypeConfig[] typesConfigs;
	private LootTable[] lootTables = new LootTable[]{};


	public Optional<WeaponTypeConfig> getByItemId(String itemId) {
		if (typesConfigs == null || typesConfigs.length == 0) {
			return Optional.empty();
		}

		return Arrays.stream(typesConfigs)
				.filter(c -> contains(c.getItemIds(), itemId))
				.findFirst();
	}

	public LootTable getLootTableById(String id) {
		for (LootTable lootTable : lootTables) {
			if (lootTable.getId().equalsIgnoreCase(id)) {
				return lootTable;
			}
		}

		return null;
	}


}
