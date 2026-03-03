package ar.ncode.plugin.ecs.component.death;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

@NoArgsConstructor
public class ConfirmedDeath implements Component<EntityStore> {

	public static final BuilderCodec<ConfirmedDeath> CODEC =
			BuilderCodec.builder(ConfirmedDeath.class, ConfirmedDeath::new)
					.build();

	public static ComponentType<EntityStore, ConfirmedDeath> componentType;

	@NullableDecl
	@Override
	public Component<EntityStore> clone() {
		return new ConfirmedDeath();
	}
}
