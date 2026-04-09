package me.mykindos.betterpvp.hub;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("UnstableApiUsage")
public class HubBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        final Path serverDirectory = resolveServerDirectory(context);
        final Path worldsDirectory = serverDirectory.resolve("worlds");

        final Path worldDirectory = worldsDirectory.resolve(BPvPWorld.MAIN_WORLD_NAME);
        final Path worldZip = worldsDirectory.resolve(BPvPWorld.MAIN_WORLD_NAME + ".zip");

        if (!Files.exists(worldZip)) {
            context.getLogger().warn("Skipped hub world restore because {} does not exist", worldZip);
            return;
        }

        try {
            deleteDirectory(worldDirectory);
            extractWorldZip(worldZip, worldsDirectory, worldDirectory);
            context.getLogger().info("Restored " + BPvPWorld.MAIN_WORLD_NAME + " from {}", worldZip);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to restore " + BPvPWorld.MAIN_WORLD_NAME + " from " + worldZip, ex);
        }
    }

    private Path resolveServerDirectory(BootstrapContext context) {
        final Path pluginSourceParent = context.getPluginSource().toAbsolutePath().getParent();
        if (pluginSourceParent != null && pluginSourceParent.getFileName() != null
                && pluginSourceParent.getFileName().toString().equalsIgnoreCase("plugins")) {
            final Path serverRoot = pluginSourceParent.getParent();
            if (serverRoot != null) {
                return serverRoot;
            }
        }

        return Path.of("").toAbsolutePath().normalize();
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw ex;
        }
    }

    private void extractWorldZip(Path zipPath, Path worldsDirectory, Path worldDirectory) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            final boolean hasRootWorldDirectory = zipFile.stream()
                    .map(ZipEntry::getName)
                    .map(name -> name.replace('\\', '/'))
                    .anyMatch(name -> name.equals(BPvPWorld.MAIN_WORLD_NAME)
                            || name.startsWith(BPvPWorld.MAIN_WORLD_NAME + "/"));
            final Path extractionRoot = hasRootWorldDirectory ? worldsDirectory : worldDirectory;

            Files.createDirectories(extractionRoot);

            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final String entryName = entry.getName().replace('\\', '/');
                final Path target = extractionRoot.resolve(entryName).normalize();

                if (!target.startsWith(extractionRoot)) {
                    throw new IOException("Refused to extract zip entry outside world container: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }

                final Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    Files.copy(inputStream, target);
                }
            }
        }
    }
}
