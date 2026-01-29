package ar.ncode.plugin.model;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstanceConfig {

	public static final BuilderCodec<InstanceConfig> CODEC =
			BuilderCodec.builder(InstanceConfig.class, InstanceConfig::new)
					.append(new KeyedCodec<>("LootBoxes", ArrayCodec.ofBuilderCodec(LootBox.CODEC, LootBox[]::new)),
							(c, value, extraInfo) -> c.lootBoxes = value,
							(c, extraInfo) -> c.lootBoxes)
					.add()
					.build();

	LootBox[] lootBoxes;

}
