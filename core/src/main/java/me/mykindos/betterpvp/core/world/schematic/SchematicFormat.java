package me.mykindos.betterpvp.core.world.schematic;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Reads a {@link Schematic} from a particular on-disk file type. This is the extension point of the schematic
 * subsystem: implement it and register with {@link SchematicService#register(SchematicFormat)} to support a new
 * format. The built-in {@link FaweSchematicFormat} handles {@code .schem}/{@code .schematic}.
 */
public interface SchematicFormat {

    /**
     * @return a short stable identifier for this format (e.g. {@code "fawe"})
     */
    @NotNull String id();

    /**
     * @return the lowercase file extensions (without a leading dot) this format claims, e.g. {@code {"schem"}}
     */
    @NotNull Set<String> extensions();

    /**
     * Parses a schematic from a stream. The stream is owned by the caller and is not closed here.
     *
     * @param in the schematic bytes
     * @return the parsed schematic
     * @throws IOException if the stream is not a valid schematic of this format
     */
    @NotNull Schematic read(@NotNull InputStream in) throws IOException;

    /**
     * Writes a schematic to a stream. The stream is owned by the caller and is not closed here.
     * <p>
     * Optional: a format that only ever reads existing files (a legacy import path, say) may leave this unsupported.
     * Capturing from the world requires it.
     *
     * @throws UnsupportedOperationException if this format cannot be written
     */
    default void write(@NotNull OutputStream out, @NotNull Schematic schematic) throws IOException {
        throw new UnsupportedOperationException(id() + " schematics can be read but not written");
    }
}
