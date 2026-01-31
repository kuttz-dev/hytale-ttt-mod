package ar.ncode.plugin.config.instance.loot;

import ar.ncode.plugin.config.instance.SpawnPoint;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter()
@Setter()
@NoArgsConstructor()
public class LootSpawnPoint {

	public static final BuilderCodec<LootSpawnPoint> CODEC =
			BuilderCodec.builder(LootSpawnPoint.class, LootSpawnPoint::new)
					.append(new KeyedCodec<>("SpawnPoint", SpawnPoint.CODEC),
							(c, value, extraInfo) -> c.spawnPoint = value,
							(c, extraInfo) -> c.spawnPoint)
					.add()
					.append(new KeyedCodec<>("LootTables", Codec.STRING_ARRAY),
							(c, value, extraInfo) -> c.lootTables = value,
							(c, extraInfo) -> c.lootTables)
					.add()
					.append(new KeyedCodec<>("Probability", Codec.INTEGER),
							(c, value, extraInfo) -> c.probability = value,
							(c, extraInfo) -> c.probability)
					.add()
					.build();


	private SpawnPoint spawnPoint = new SpawnPoint();
	private String[] lootTables = new String[]{};
	private int probability = 100;

}
