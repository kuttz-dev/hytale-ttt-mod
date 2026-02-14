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
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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
public class GraveStoneWithNameplate implements Component<ChunkStore> {

	public static final BuilderCodec<GraveStoneWithNameplate> CODEC =
			BuilderCodec.builder(GraveStoneWithNameplate.class, GraveStoneWithNameplate::new)
					.append(new KeyedCodec<>("GraveStonePosition", Codec.INT_ARRAY),
							(c, v) -> c.graveStonePosition = v != null && v.length == 3 ? new Vector3i(v[0], v[1],
									v[2]) : null,
							c -> c.graveStonePosition == null ? new int[]{} : new int[]{c.graveStonePosition.x,
									c.graveStonePosition.y, c.graveStonePosition.z})
					.add()
					.append(new KeyedCodec<>("NamePlatePosition", Codec.DOUBLE_ARRAY),
							(c, v) -> c.namePlatePosition = v != null && v.length == 3 ?
									new Vector3d(v[0], v[1], v[2]) : null,
							c -> c.namePlatePosition == null ? new double[]{} : new double[]{c.namePlatePosition.x, c.namePlatePosition.y, c.namePlatePosition.z})
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

	public static ComponentType<ChunkStore, GraveStoneWithNameplate> componentType;

	private Vector3i graveStonePosition;
	private Vector3d namePlatePosition;
	private Ref<EntityStore> namePlateReference;
	private Ref<EntityStore> deadPlayerReference;
	private DamageCause causeOfDeath;
	private CustomRole deadPlayerRole;
	private String deadPlayerName;
	private String timeOfDeath;


	@NullableDecl
	@Override
	public Component<ChunkStore> clone() {
		return new GraveStoneWithNameplate(graveStonePosition, namePlatePosition, namePlateReference,
				deadPlayerReference, causeOfDeath, deadPlayerRole, deadPlayerName, timeOfDeath);
	}

	@NullableDecl
	@Override
	public Component<ChunkStore> cloneSerializable() {
		return Component.super.cloneSerializable();
	}

	public void setGraveAndNameplatePosition(Vector3i graveStonePosition) {
		this.graveStonePosition = graveStonePosition;
		this.namePlatePosition = graveStonePosition.toVector3d()
				.add(0.5F, 1.2, 0.5F);
	}
}
