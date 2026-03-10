package ar.ncode.plugin.asset;

import ar.ncode.plugin.exception.ConfigError;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Stream;

public class WorldPreviewLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private WorldPreviewLoader() {
    }

    public static void loadInstancesAsAssets(Path templatesPath, Path pluginDataPath) throws Exception {
        if (!Files.exists(templatesPath)) {
            return;
        }

        // Asset folder structure
        Path assetsRoot = pluginDataPath.resolve("assets");
        Path imagesFolder = assetsRoot.resolve("Common/UI/Custom/Images/Worlds");
        Files.createDirectories(imagesFolder);

        Path instancesFolder = assetsRoot.resolve("Server/Instances");
        Files.createDirectories(instancesFolder);

        int maps = 0;
        try (Stream<Path> worlds = Files.list(templatesPath)) {
            for (Path world : (Iterable<Path>) worlds::iterator) {
                if (!Files.isDirectory(world)) {
                    continue;
                }

                String worldName = world.getFileName().toString();
                Path preview = world.resolve("preview.png");
                Path instance = world.resolve("instance.bson");
                Path chunks = world.resolve("chunks");

                if (!Files.exists(preview) || !Files.exists(instance) || !Files.exists(chunks)) {
                    LOGGER.atWarning().log("World template %s is missing required files (preview.png, instance.bson, chunks/) - skipping", worldName);
                    continue;
                }

                Path previewOutput = imagesFolder.resolve(worldName + ".png");
                Path instanceOutput = instancesFolder.resolve(worldName);
                Files.createDirectories(instanceOutput);

                try {
                    Files.copy(preview, previewOutput, StandardCopyOption.REPLACE_EXISTING);
                    copyDirectories(world, instanceOutput);
                    copyDirectories(world.resolve("chunks"), instanceOutput.resolve("chunks"));

                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to copy world template for world %s - %s", worldName, e);
                    continue;
                }

                maps++;
            }

            if (maps == 0) {
                throw new ConfigError("There are no maps configured, game mode can not start.");
            }

            // Register as asset pack
            PluginManifest manifest = new PluginManifest();
            manifest.setGroup("ncode");
            manifest.setName("ttt-worlds_assets");
            manifest.setVersion(new Semver(1, 0, 0));

            AssetModule.get().registerPack("worlds_assets", assetsRoot, manifest, false);

        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to load worlds assets - %s", e);
        }

        if (maps == 0) {
            throw new ConfigError("There are no maps configured, game mode can not start.");
        }
    }

    public static void copyDirectories(Path sourceDir, Path targetDir) throws IOException {
        Objects.requireNonNull(sourceDir, "sourceDir must not be null");
        Objects.requireNonNull(targetDir, "targetDir must not be null");

        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Source is not a directory: " + sourceDir);
        }

        Files.createDirectories(targetDir);

        try (Stream<Path> paths = Files.list(sourceDir)) {
            for (Path sourceFile : paths.toList()) {
                if (!Files.isRegularFile(sourceFile)) {
                    continue;
                }

                Path targetFile = targetDir.resolve(sourceFile.getFileName());

                Files.copy(
                        sourceFile,
                        targetFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                );
            }
        }
    }
}
