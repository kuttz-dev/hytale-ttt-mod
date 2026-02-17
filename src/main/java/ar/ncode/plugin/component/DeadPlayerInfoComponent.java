package ar.ncode.plugin.component;

import ar.ncode.plugin.config.CustomRole;
import ar.ncode.plugin.model.DamageCause;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeadPlayerInfoComponent implements Component<EntityStore> {

	public static final BuilderCodec<DeadPlayerInfoComponent> CODEC =
			BuilderCodec.builder(DeadPlayerInfoComponent.class, DeadPlayerInfoComponent::new)
					.append(new KeyedCodec<>("Position", Codec.INT_ARRAY),
							(c, v) -> c.position = v != null && v.length == 3 ? new Vector3i(v[0], v[1],
									v[2]) : null,
							c -> c.position == null ? new int[]{} : new int[]{c.position.x,
									c.position.y, c.position.z})
					.add()
					.append(new KeyedCodec<>("NamePlatePosition", Codec.DOUBLE_ARRAY),
							(c, v) -> c.namePlatePosition = v != null && v.length == 3 ?
									new Vector3d(v[0], v[1], v[2]) : null,
							c -> c.namePlatePosition == null ? new double[]{} : new double[]{c.namePlatePosition.x, c.namePlatePosition.y, c.namePlatePosition.z})
					.add()
					.append(new KeyedCodec<>("Rotation", Codec.FLOAT_ARRAY),
							(c, v) -> c.rotation = v != null && v.length == 3 ?
									new Vector3f(v[0], v[1], v[2]) : null,
							c -> c.rotation == null ? new float[]{} : new float[]{c.rotation.x, c.rotation.y, c.rotation.z})
					.add()
					.append(new KeyedCodec<>("CauseOfDeath", Codec.STRING),
							(c, v) -> c.causeOfDeath = "".equals(v) ? null : DamageCause.valueOf(v.toUpperCase()),
							c -> c.causeOfDeath == null ? "" : c.causeOfDeath.name())
					.add()
					.append(new KeyedCodec<>("DeadPlayerRole", CustomRole.CODEC),
							(c, v) -> c.deadPlayerRole = v,
							c -> c.deadPlayerRole
					)
					.add()
					.append(new KeyedCodec<>("DeadPlayerName", Codec.STRING),
							(c, v) -> c.deadPlayerName = v,
							c -> c.deadPlayerName)
					.add()
					.append(new KeyedCodec<>("TimeOfDeath", Codec.STRING),
							(c, v) -> c.timeOfDeath = v,
							c -> c.timeOfDeath)
					.add()
					.build();

	public static ComponentType<EntityStore, DeadPlayerInfoComponent> componentType;

	private Vector3i position;
	private Vector3d namePlatePosition;
	private Vector3f rotation;
	private Ref<EntityStore> namePlateReference;
	private Ref<EntityStore> deadPlayerReference;
	private DamageCause causeOfDeath;
	private CustomRole deadPlayerRole;
	private String deadPlayerName;
	private String timeOfDeath;


	@NullableDecl
	@Override
	public Component<EntityStore> clone() {
		return new DeadPlayerInfoComponent(position, namePlatePosition, rotation, namePlateReference,
				deadPlayerReference, causeOfDeath, deadPlayerRole, deadPlayerName, timeOfDeath);
	}

	@NullableDecl
	@Override
	public Component<EntityStore> cloneSerializable() {
		return Component.super.cloneSerializable();
	}

	public void setGraveAndNameplatePosition(Vector3i graveStonePosition) {
		this.position = graveStonePosition;
		this.namePlatePosition = graveStonePosition.toVector3d()
				.add(0.5F, 1.2, 0.5F);
	}
}
