package ar.ncode.plugin.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.Arrays;
import java.util.Optional;

import static com.hypixel.hytale.common.util.ArrayUtil.contains;

public class WeaponTypeConfigs {

	public static final BuilderCodec<WeaponTypeConfigs> CODEC =
			BuilderCodec.builder(WeaponTypeConfigs.class, WeaponTypeConfigs::new)
					.append(new KeyedCodec<>("Configs", ArrayCodec.ofBuilderCodec(WeaponTypeConfig.CODEC,
									WeaponTypeConfig[]::new)),
							(c, value, extraInfo) -> c.configs = value,
							(c, extraInfo) -> c.configs)
					.add()
					.build();

	private WeaponTypeConfig[] configs;


	public Optional<WeaponTypeConfig> getByItemId(String itemId) {
		return Arrays.stream(configs)
				.filter(c -> contains(c.getItemIds(), itemId))
				.findFirst();
	}

}
