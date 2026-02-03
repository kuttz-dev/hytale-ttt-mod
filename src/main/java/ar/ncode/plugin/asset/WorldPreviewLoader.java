package ar.ncode.plugin.asset;

import ar.ncode.plugin.model.WorldPreview;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class WorldPreviewLoader {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private WorldPreviewLoader() {
	}

	public static List<WorldPreview> load(Path universePath, Path pluginDataPath) throws Exception {
		if (!Files.exists(universePath)) {
			return new ArrayList<>();
		}

		// Asset folder structure
		Path assetsRoot = pluginDataPath.resolve("assets");
		Path imagesFolder = assetsRoot.resolve("Common/UI/Custom/Images/Worlds");
		Files.createDirectories(imagesFolder);

		Path instancesFolder = assetsRoot.resolve("Server/Instances");
		Files.createDirectories(instancesFolder);

		List<WorldPreview> result = new ArrayList<>();

		try (Stream<Path> worlds = Files.list(universePath)) {
			for (Path world : (Iterable<Path>) worlds::iterator) {
				if (!Files.isDirectory(world)) {
					continue;
				}

				Path preview = world.resolve("preview.png");
				Path instance = world.resolve("instance.bson");
				Path chunks = world.resolve("chunks");

				if (!Files.exists(preview) || !Files.exists(instance) || !Files.exists(chunks)) {
					continue;
				}

				String worldName = world.getFileName().toString();
				Path previewOutput = imagesFolder.resolve(worldName + ".png");
				Path instanceOutput = instancesFolder.resolve(worldName);
				Files.createDirectories(instanceOutput);

				Files.copy(preview, previewOutput, StandardCopyOption.REPLACE_EXISTING);
				copyFiles(world, instanceOutput);
				copyFiles(world.resolve("chunks"), instanceOutput.resolve("chunks"));
				result.add(new WorldPreview(worldName));
			}

			if (result.isEmpty()) {
				return result;
			}

			// Register as asset pack
			PluginManifest manifest = new PluginManifest();
			manifest.setGroup("ncode");
			manifest.setName("ttt-worlds_assets");
			manifest.setVersion(new Semver(1, 0, 0));

			AssetModule.get().registerPack("worlds_assets", assetsRoot, manifest);

		} catch (Exception ignored) {
			LOGGER.atSevere().log("Failed to load worlds assets - {}", ignored);
		}

		return result;
	}

	public static void copyFiles(Path sourceDir, Path targetDir) throws IOException {
		if (!Files.isDirectory(sourceDir)) {
			throw new IllegalArgumentException("Source is not a directory: " + sourceDir);
		}

		Files.createDirectories(targetDir);

		try (Stream<Path> paths = Files.list(sourceDir)) {
			paths.filter(Files::isRegularFile)
					.forEach(sourceFile -> {
						Path targetFile = targetDir.resolve(sourceFile.getFileName());
						try {
							Files.copy(
									sourceFile,
									targetFile,
									StandardCopyOption.REPLACE_EXISTING
							);
						} catch (IOException e) {
							throw new RuntimeException(
									"Failed to copy " + sourceFile + " to " + targetFile, e
							);
						}
					});
		}
	}
}
