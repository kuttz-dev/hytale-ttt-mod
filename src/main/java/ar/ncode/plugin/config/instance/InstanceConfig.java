package ar.ncode.plugin.config.instance;

import ar.ncode.plugin.config.instance.loot.LootSpawnPoint;
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
					.append(new KeyedCodec<>("LootSpawnPoints", ArrayCodec.ofBuilderCodec(LootSpawnPoint.CODEC,
									LootSpawnPoint[]::new)),
							(c, value, extraInfo) -> c.lootSpawnPoints = value,
							(c, extraInfo) -> c.lootSpawnPoints)
					.add()
					.append(new KeyedCodec<>("PlayerSpawnPoints", ArrayCodec.ofBuilderCodec(SpawnPoint.CODEC,
									SpawnPoint[]::new)),
							(c, value, extraInfo) -> c.playerSpawnPoints = value,
							(c, extraInfo) -> c.playerSpawnPoints)
					.add()
					.build();

	LootSpawnPoint[] lootSpawnPoints = new LootSpawnPoint[]{};
	SpawnPoint[] playerSpawnPoints = new SpawnPoint[]{};

}
