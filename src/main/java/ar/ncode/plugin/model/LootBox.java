package ar.ncode.plugin.model;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

@Setter
@NoArgsConstructor
public class LootBox {

	public static final BuilderCodec<LootBox> CODEC =
			BuilderCodec.builder(LootBox.class, LootBox::new)
					.append(new KeyedCodec<>("Position", Vector3d.CODEC),
							(c, value, extraInfo) -> c.position = value == null ? new Vector3d() :
									c.position.assign(value),
							(c, extraInfo) -> c.position)
					.add()
					.append(new KeyedCodec<>("Rotation", Vector3f.CODEC),
							(c, value, extraInfo) -> c.rotation = value == null ? new Vector3f() :
									c.rotation.assign(value),
							(c, extraInfo) -> c.rotation)
					.add()
					.append(new KeyedCodec<>("PossibleLoot", Codec.STRING_ARRAY),
							(c, value, extraInfo) -> c.possibleLoot = value,
							(c, extraInfo) -> c.possibleLoot)
					.add()
					.build();
	private Vector3d position = new Vector3d();
	private Vector3f rotation = new Vector3f();
	private String[] possibleLoot = new String[]{};
	public LootBox(Vector3d position, Vector3f rotation) {
		this.position = position;
		this.rotation = rotation;
	}

	@Override
	public String toString() {
		return "LootBox{" +
				"position={" + position.x + "," + position.y + "," + position.z + "}" +
				", rotation={" + rotation.x + "," + rotation.y + "," + rotation.z + "}" +
				", possibleLoot=" + Arrays.toString(possibleLoot) +
				'}';
	}
}
