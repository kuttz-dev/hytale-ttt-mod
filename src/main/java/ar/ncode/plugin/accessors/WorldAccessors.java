package ar.ncode.plugin.accessors;

import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.model.PlayerComponents;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.ArrayList;
import java.util.List;

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


	public static List<PlayerComponents> getPlayersAt(World world) {
		List<PlayerComponents> result = new ArrayList<>();
		for (PlayerRef playerRef : world.getPlayerRefs()) {
			var reference = playerRef.getReference();
			if (reference == null || !reference.isValid()) continue;

			var player = reference.getStore().getComponent(reference, Player.getComponentType());
			var playerInfo = reference.getStore().getComponent(reference, PlayerGameModeInfo.componentType);

			if (player == null || playerInfo == null) continue;

			result.add(new PlayerComponents(player, playerRef, playerInfo, reference));
		}

		return result;
	}

	public static String getWorldNameForInstance(World world) {
		if (world.getWorldConfig().getDisplayName() == null) return null;

		return world.getWorldConfig().getDisplayName()
				.replace(" ", "_")
				.toLowerCase();
	}


}
