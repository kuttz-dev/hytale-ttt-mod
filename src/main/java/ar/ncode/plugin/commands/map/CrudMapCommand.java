package ar.ncode.plugin.commands.map;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.accessors.WorldAccessors;
import ar.ncode.plugin.asset.WorldPreviewLoader;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeDoublePosition;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ar.ncode.plugin.model.CustomPermissions.TTT_MAP_CRUD;

public class CrudMapCommand {


	public static class CreateMapCommand extends AbstractAsyncCommand {

		RequiredArg<String> mapName = this.withRequiredArg("name", "New map name", ArgTypes.STRING);
		OptionalArg<RelativeDoublePosition> spawnPosition = this.withOptionalArg("name", "New map name", ArgTypes.RELATIVE_POSITION);

		public CreateMapCommand() {
			super("create", "Creates a new map with required folder and files");
			addAliases("add");
			requirePermission(TTT_MAP_CRUD);
		}

		protected void executeSync(@Nonnull CommandContext ctx) throws Exception {
			var name = mapName.get(ctx);
			var safeName = WorldAccessors.getSafeWorldName(name);

			Path mapFolder = TroubleInTrorkTownPlugin.instance.getDataDirectory().resolve("maps", safeName);
			Files.createDirectories(mapFolder);

			// Try to copy templates from classpath resources (/templates/map)
			URL resource = TroubleInTrorkTownPlugin.class.getClassLoader().getResource("templates/map");
			if (resource == null) {
				return;
			}
			try {
				URI uri = resource.toURI();
				if ("file".equalsIgnoreCase(uri.getScheme())) {
					Path templatesRoot = Paths.get(uri);
					try (Stream<Path> stream = Files.walk(templatesRoot)) {
						stream.forEach(src -> {
							try {
								Path rel = templatesRoot.relativize(src);
								Path dst = mapFolder.resolve(rel.toString());
								if (Files.isDirectory(src)) {
									Files.createDirectories(dst);
								} else {
									Files.createDirectories(dst.getParent());
									Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
								}
							} catch (IOException ex) {
								throw new RuntimeException(ex);
							}
						});
					}
					return;
				} else if (uri.getScheme().startsWith("jar")) {
					// Resource inside JAR
					try (FileSystem fs = FileSystems.newFileSystem(uri, new HashMap<>())) {
						Path templatesRoot = fs.getPath("/templates/map");
						try (Stream<Path> stream = Files.walk(templatesRoot)) {
							stream.forEach(src -> {
								try (InputStream in = Files.isDirectory(src) ? null : Files.newInputStream(src)) {
									Path rel = templatesRoot.relativize(src);
									Path dst = mapFolder.resolve(rel.toString());
									if (Files.isDirectory(src)) {
										Files.createDirectories(dst);
									} else {
										Files.createDirectories(dst.getParent());
										Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
									}
								} catch (IOException ex) {
									throw new RuntimeException(ex);
								}
							});
						}
					}
				}
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}

			// Replace WORLD_NAME token in instance.bson with the provided map name (if the file exists)
			Path instanceFile = mapFolder.resolve("instance.bson");
			if (Files.exists(instanceFile) && !Files.isDirectory(instanceFile)) {
				try {
					byte[] bytes = Files.readAllBytes(instanceFile);
					String text = new String(bytes);
					String replaced = text.replace("WORLD_NAME", name);
					Files.write(instanceFile, replaced.getBytes());
				} catch (IOException e) {
					ctx.sendMessage(Message.raw("Created map folder but failed to update instance.bson: " + e.getMessage()));
					// proceed without failing to keep behavior similar to previous implementation
				}
			}

			TroubleInTrorkTownPlugin.worldPreviews = WorldPreviewLoader.load(
					TroubleInTrorkTownPlugin.instance.templatesPath,
					TroubleInTrorkTownPlugin.instance.getDataDirectory()
			);

			TroubleInTrorkTownPlugin.instance.loadMapsEntries();
			TroubleInTrorkTownPlugin.instance.loadMapsConfig();

			ctx.sendMessage(Message.raw("Map " + name + " created."));
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				try {
					executeSync(ctx);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	public static class ReadMapsCommand extends AbstractAsyncCommand {

		OptionalArg<String> mapName = this.withOptionalArg("name", "Map name to show details for", ArgTypes.STRING);

		public ReadMapsCommand() {
			super("read", "Lists maps or shows details for a specific map");
			addAliases("list", "ls", "show");
			requirePermission(TTT_MAP_CRUD);
		}

		protected void executeSync(@Nonnull CommandContext ctx) throws IOException {
			if (TroubleInTrorkTownPlugin.instance == null) {
				ctx.sendMessage(Message.raw("Plugin instance not available."));
				return;
			}

			Path templatesPath = TroubleInTrorkTownPlugin.instance.templatesPath;
			if (!Files.exists(templatesPath)) {
				ctx.sendMessage(Message.raw("No maps directory found."));
				return;
			}

			var name = mapName.get(ctx);
			if (name == null) {
				// list maps
				try (Stream<Path> stream = Files.list(templatesPath)) {
					var names = stream
							.filter(Files::isDirectory)
							.map(p -> p.getFileName().toString())
							.collect(Collectors.toList());

					if (names.isEmpty()) {
						ctx.sendMessage(Message.raw("No maps found."));
						return;
					}

					ctx.sendMessage(Message.raw("Available maps: " + String.join(", ", names)));
				}
				return;
			}

			// show details for a specific map
			Path mapPath = templatesPath.resolve(name);
			if (!Files.exists(mapPath) || !Files.isDirectory(mapPath)) {
				ctx.sendMessage(Message.raw("Map '" + name + "' not found."));
				return;
			}

			// list files inside the map folder
			try (Stream<Path> stream = Files.list(mapPath)) {
				ctx.sendMessage(Message.raw("Map: " + name));
				Path cfg = mapPath.resolve("config.json");
				if (Files.exists(cfg) && !Files.isDirectory(cfg)) {
				}

			} catch (IOException e) {
				ctx.sendMessage(Message.raw("Failed to read map details: " + e.getMessage()));
			}
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				try {
					executeSync(ctx);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	// Update command - rename folder and update config.json contents replacing old name with new name
	public static class UpdateMapCommand extends AbstractAsyncCommand {

		RequiredArg<String> oldNameArg = this.withRequiredArg("oldName", "Existing map name", ArgTypes.STRING);
		RequiredArg<String> newNameArg = this.withRequiredArg("newName", "New map name", ArgTypes.STRING);

		public UpdateMapCommand() {
			super("update", "Rename a map folder and update its config");
			addAliases("rename");
			requirePermission(TTT_MAP_CRUD);
		}

		protected void executeSync(@Nonnull CommandContext ctx) throws Exception {
			if (TroubleInTrorkTownPlugin.instance == null) {
				ctx.sendMessage(Message.raw("Plugin instance not available."));
				return;
			}

			String oldName = oldNameArg.get(ctx);
			String newName = newNameArg.get(ctx);
			var safeOld = WorldAccessors.getSafeWorldName(oldName);
			var safeNew = WorldAccessors.getSafeWorldName(newName);

			Path mapsRoot = TroubleInTrorkTownPlugin.instance.templatesPath;
			Path oldPath = mapsRoot.resolve(safeOld);
			Path newPath = mapsRoot.resolve(safeNew);

			if (!Files.exists(oldPath) || !Files.isDirectory(oldPath)) {
				ctx.sendMessage(Message.raw("Map '" + oldName + "' does not exist."));
				return;
			}

			if (Files.exists(newPath)) {
				ctx.sendMessage(Message.raw("A map with the new name '" + newName + "' already exists."));
				return;
			}

			try {
				Files.move(oldPath, newPath);
			} catch (IOException e) {
				ctx.sendMessage(Message.raw("Failed to rename map folder: " + e.getMessage()));
				return;
			}

			// Update config.json: replace occurrences of the old name with the new one
			Path cfg = newPath.resolve("config.json");
			if (Files.exists(cfg) && !Files.isDirectory(cfg)) {
				try {
					String content = new String(Files.readAllBytes(cfg));
					String updated = content.replace(oldName, newName).replace(safeOld, safeNew);
					Files.write(cfg, updated.getBytes());
				} catch (IOException e) {
					ctx.sendMessage(Message.raw("Renamed folder but failed to update config.json: " + e.getMessage()));
					return;
				}
			}

			TroubleInTrorkTownPlugin.worldPreviews = WorldPreviewLoader.load(
					TroubleInTrorkTownPlugin.instance.templatesPath,
					TroubleInTrorkTownPlugin.instance.getDataDirectory()
			);

			TroubleInTrorkTownPlugin.instance.loadMapsEntries();
			TroubleInTrorkTownPlugin.instance.loadMapsConfig();

			ctx.sendMessage(Message.raw("Map renamed from '" + oldName + "' to '" + newName + "'."));
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				try {
					executeSync(ctx);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	// Delete command - requires explicit confirmation word
	public static class DeleteMapCommand extends AbstractAsyncCommand {

		RequiredArg<String> mapName = this.withRequiredArg("name", "Map name to delete", ArgTypes.STRING);
		OptionalArg<Boolean> confirmation = this.withOptionalArg("confirm", "Type 'confirm' to delete", ArgTypes.BOOLEAN);

		public DeleteMapCommand() {
			super("delete", "Delete a map (requires confirmation)");
			addAliases("del", "rm");
			requirePermission(TTT_MAP_CRUD);
		}

		protected void executeSync(@Nonnull CommandContext ctx) throws Exception {
			if (TroubleInTrorkTownPlugin.instance == null) {
				ctx.sendMessage(Message.raw("Plugin instance not available."));
				return;
			}

			String name = mapName.get(ctx);
			var confirm = confirmation.get(ctx);
			Path mapsRoot = TroubleInTrorkTownPlugin.instance.templatesPath;
			Path target = mapsRoot.resolve(WorldAccessors.getSafeWorldName(name));

			if (!Files.exists(target) || !Files.isDirectory(target)) {
				ctx.sendMessage(Message.raw("Map '" + name + "' does not exist."));
				return;
			}

			if (Boolean.FALSE.equals(confirm)) {
				ctx.sendMessage(Message.raw("This will delete map '" + name + "' and cannot be undone. Re-run with: /map delete " + name + " confirm"));
				return;
			}

			try (Stream<Path> stream = Files.walk(target)) {
				stream.sorted(Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			} catch (RuntimeException e) {
				ctx.sendMessage(Message.raw("Failed to delete map: " + e.getCause().getMessage()));
				return;
			}

			TroubleInTrorkTownPlugin.worldPreviews = WorldPreviewLoader.load(
					TroubleInTrorkTownPlugin.instance.templatesPath,
					TroubleInTrorkTownPlugin.instance.getDataDirectory()
			);

			TroubleInTrorkTownPlugin.instance.loadMapsEntries();
			TroubleInTrorkTownPlugin.instance.loadMapsConfig();

			ctx.sendMessage(Message.raw("Map '" + name + "' deleted."));
		}

		@Nonnull
		@Override
		protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext ctx) {
			return CompletableFuture.runAsync(() -> {
				try {
					executeSync(ctx);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

}
