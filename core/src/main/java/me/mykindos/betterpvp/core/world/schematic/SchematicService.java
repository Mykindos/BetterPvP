package me.mykindos.betterpvp.core.world.schematic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches {@link Schematic}s from the module's {@code schematics/} data folder, resolving the
 * {@link SchematicFormat} by file extension. The {@link FaweSchematicFormat} is registered by default; additional
 * formats can be plugged in via {@link #register(SchematicFormat)}.
 */
@Singleton
@CustomLog
public class SchematicService {

    private final File folder;
    private final Map<String, SchematicFormat> formatsByExtension = new HashMap<>();
    private final Map<String, Schematic> cache = new ConcurrentHashMap<>();

    @Inject
    public SchematicService(@NotNull Core core) {
        this(new File(core.getDataFolder(), "schematics"));
        register(new FaweSchematicFormat());
    }

    /**
     * Test/advanced constructor: point the service at an explicit folder with no default formats registered.
     */
    SchematicService(@NotNull File folder) {
        this.folder = folder;
    }

    /**
     * Registers a format under each of its {@link SchematicFormat#extensions() extensions}. Later registrations win
     * for a shared extension.
     */
    public void register(@NotNull SchematicFormat format) {
        for (String extension : format.extensions()) {
            formatsByExtension.put(extension.toLowerCase(Locale.ROOT), format);
        }
    }

    /**
     * @param fileName a file name, with or without directory, e.g. {@code "willow.schem"}
     * @return the format whose extension matches the file name, if any
     */
    public @NotNull Optional<SchematicFormat> formatFor(@NotNull String fileName) {
        final int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(formatsByExtension.get(fileName.substring(dot + 1).toLowerCase(Locale.ROOT)));
    }

    /**
     * Loads a schematic by name from the schematics folder. {@code name} may include an extension (used verbatim) or
     * omit it (the folder is searched for any registered extension). Results are cached by lower-cased name.
     *
     * @param name the schematic name, e.g. {@code "willow_stump"} or {@code "willow_stump.schem"}
     * @return the parsed schematic, or empty if no file matched or it failed to parse
     */
    public @NotNull Optional<Schematic> load(@NotNull String name) {
        final String cacheKey = name.toLowerCase(Locale.ROOT);
        final Schematic cached = cache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        final Optional<File> fileOpt = resolveFile(name);
        if (fileOpt.isEmpty()) {
            log.warn("No schematic file found for '{}' in {}", name, folder).submit();
            return Optional.empty();
        }

        final File file = fileOpt.get();
        final Optional<SchematicFormat> format = formatFor(file.getName());
        if (format.isEmpty()) {
            log.warn("No schematic format registered for file '{}'", file.getName()).submit();
            return Optional.empty();
        }

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            final Schematic schematic = format.get().read(in);
            cache.put(cacheKey, schematic);
            return Optional.of(schematic);
        } catch (IOException exception) {
            log.error("Failed to read schematic '{}'", file.getName(), exception).submit();
            return Optional.empty();
        }
    }

    private @NotNull Optional<File> resolveFile(@NotNull String name) {
        final File direct = new File(folder, name);
        if (direct.isFile()) {
            return Optional.of(direct);
        }
        for (String extension : formatsByExtension.keySet()) {
            final File candidate = new File(folder, name + "." + extension);
            if (candidate.isFile()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * Drops all cached schematics so the next {@link #load(String)} re-reads from disk.
     */
    public void clearCache() {
        cache.clear();
    }
}
