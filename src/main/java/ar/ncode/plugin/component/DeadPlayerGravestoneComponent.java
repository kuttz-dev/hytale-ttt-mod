package ar.ncode.plugin.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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
public class DeadPlayerGravestoneComponent implements Component<ChunkStore> {

	public static final BuilderCodec<DeadPlayerGravestoneComponent> CODEC =
			BuilderCodec.builder(DeadPlayerGravestoneComponent.class, DeadPlayerGravestoneComponent::new)
					.append(new KeyedCodec<>("DeadPlayerInfoComponent", DeadPlayerInfoComponent.CODEC),
							(c, v) -> c.deadPlayerInfoComponent = v,
							c -> c.deadPlayerInfoComponent)
					.add()
					.build();

	public static ComponentType<ChunkStore, DeadPlayerGravestoneComponent> componentType;

	private DeadPlayerInfoComponent deadPlayerInfoComponent;

	@NullableDecl
	@Override
	public Component<ChunkStore> clone() {
		return new DeadPlayerGravestoneComponent(deadPlayerInfoComponent);
	}

	@NullableDecl
	@Override
	public Component<ChunkStore> cloneSerializable() {
		return Component.super.cloneSerializable();
	}

}
