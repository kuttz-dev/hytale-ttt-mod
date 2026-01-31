package ar.ncode.plugin.config.instance;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter()
@Setter()
@NoArgsConstructor()
public class SpawnPoint {

	public static final BuilderCodec<SpawnPoint> CODEC =
			BuilderCodec.builder(SpawnPoint.class, SpawnPoint::new)
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
					.build();


	private Vector3d position = new Vector3d();
	private Vector3f rotation = new Vector3f();

	public SpawnPoint(Vector3d position, Vector3f rotation) {
		this.position = position;
		this.rotation = rotation;
	}

	@Override
	public String toString() {
		return "LootBox{" +
				"position={" + position.x + "," + position.y + "," + position.z + "}" +
				", rotation={" + rotation.x + "," + rotation.y + "," + rotation.z + "}" +
				'}';
	}
}
