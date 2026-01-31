package ar.ncode.plugin;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class WorldAccessors {

	public static Ref<ChunkStore> getBlockEntityRefAt(World world, Vector3i position) {
		long index = ChunkUtil.indexChunkFromBlock(position.x, position.z);
		return world.getChunkStore().getChunkReference(index);
	}

	public static <T extends Component<ChunkStore>> T getBlockComponentAt(
			World world, Vector3i position, ComponentType<ChunkStore, T> componentType
	) {
		if (componentType == null) {
			return null;
		}
		long index = ChunkUtil.indexChunkFromBlock(position.x, position.z);
		ChunkStore chunkStore = world.getChunkStore();
		Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(index);
		if (chunkRef == null) {
			return null;
		}

		return chunkStore.getStore().getComponent(chunkRef, componentType);

	}
}
