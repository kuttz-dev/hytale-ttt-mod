package ar.ncode.plugin.ecs.component.death;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

@NoArgsConstructor
public class LostInCombat implements Component<EntityStore> {

	public static final BuilderCodec<LostInCombat> CODEC =
			BuilderCodec.builder(LostInCombat.class, LostInCombat::new)
					.build();

	public static ComponentType<EntityStore, LostInCombat> componentType;

	@NullableDecl
	@Override
	public Component<EntityStore> clone() {
		return new LostInCombat();
	}
}
