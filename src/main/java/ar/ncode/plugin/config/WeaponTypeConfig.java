package ar.ncode.plugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;

@Getter
public class WeaponTypeConfig {

	public static final BuilderCodec<WeaponTypeConfig> CODEC =
			BuilderCodec.builder(WeaponTypeConfig.class, WeaponTypeConfig::new)
					.append(new KeyedCodec<>("TypeId", Codec.STRING),
							(config, value, extraInfo) -> config.typeId = value,
							(config, extraInfo) -> config.typeId)
					.add()
					.append(new KeyedCodec<>("ItemIds", Codec.STRING_ARRAY),
							(config, value, extraInfo) -> config.itemIds = value,
							(config, extraInfo) -> config.itemIds)
					.add()
					.append(new KeyedCodec<>("AllowedItemsOfSameType", Codec.INTEGER),
							(config, value, extraInfo) -> config.allowedItemsOfSameType = value,  // Setter
							(config, extraInfo) -> config.allowedItemsOfSameType)                     // Getter
					.add()
					.build();

	private String typeId;
	private String[] itemIds;
	private int allowedItemsOfSameType = 1;

}
