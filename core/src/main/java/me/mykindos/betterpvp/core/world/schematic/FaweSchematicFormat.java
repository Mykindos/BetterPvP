package me.mykindos.betterpvp.core.world.schematic;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads Sponge/MCEdit {@code .schem}/{@code .schematic} files via FastAsyncWorldEdit and converts them into the
 * Bukkit-only {@link Schematic} model. Format detection is by trial: the candidate readers are attempted in order
 * until one parses, so a single {@code .schem} extension covers every Sponge schematic version.
 */
public class FaweSchematicFormat implements SchematicFormat {

    @SuppressWarnings("deprecation") // SPONGE_V2_SCHEMATIC is deprecated but still needed to read legacy .schem files
    private static final List<ClipboardFormat> CANDIDATES = List.of(
            BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC,
            BuiltInClipboardFormat.SPONGE_V2_SCHEMATIC,
            BuiltInClipboardFormat.MCEDIT_SCHEMATIC);

    @Override
    public @NotNull String id() {
        return "fawe";
    }

    @Override
    public @NotNull Set<String> extensions() {
        return Set.of("schem", "schematic");
    }

    @Override
    public @NotNull Schematic read(@NotNull InputStream in) throws IOException {
        final byte[] bytes = in.readAllBytes();
        IOException last = null;
        for (ClipboardFormat format : CANDIDATES) {
            try (ClipboardReader reader = format.getReader(new ByteArrayInputStream(bytes))) {
                return convert(reader.read());
            } catch (IOException exception) {
                last = exception;
            } catch (RuntimeException exception) {
                last = new IOException("Failed reading as " + format.getName(), exception);
            }
        }
        throw last != null ? last : new IOException("No clipboard format could read the schematic");
    }

    private @NotNull Schematic convert(@NotNull Clipboard clipboard) {
        final Region region = clipboard.getRegion();
        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 dimensions = clipboard.getDimensions();
        final BlockVector3 origin = clipboard.getOrigin();

        final List<Schematic.PlacedBlock> blocks = new ArrayList<>();
        for (BlockVector3 position : region) {
            final BlockState state = clipboard.getBlock(position);
            final BlockData data = BukkitAdapter.adapt(state);
            blocks.add(new Schematic.PlacedBlock(
                    position.x() - min.x(),
                    position.y() - min.y(),
                    position.z() - min.z(),
                    data));
        }
        // The clipboard origin (the //copy position) becomes the anchor that lands on the paste location.
        return new Schematic(dimensions.x(), dimensions.y(), dimensions.z(),
                origin.x() - min.x(), origin.y() - min.y(), origin.z() - min.z(), blocks);
    }
}
