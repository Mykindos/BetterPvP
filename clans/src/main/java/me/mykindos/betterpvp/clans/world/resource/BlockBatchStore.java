package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Crash-surviving record of a <em>batch of blocks</em> stored under an id — the precise set of (position, block data)
 * pairs needed to put a region of the world back exactly as it was. A schematic-driven archetype (currently {@link
 * me.mykindos.betterpvp.clans.world.resource.archetype.TreeArchetype TreeArchetype}) saves the undo for the single
 * animation frame it has pasted, so a restart can back the frame out cleanly instead of blindly airing the whole
 * footprint — which would erase tall grass, flowers and other decoration sitting in cells the frame overlaps.
 * <p>
 * One file per batch under {@code scenes/cache/block-batches/}, keyed by a persistent {@link UUID}; a thin facade over
 * the generic {@link ResourceCacheStore} that owns the {@link Batch} shape, the store owns the I/O.
 */
@Singleton
@CustomLog
public class BlockBatchStore {

    private final ResourceCacheStore<Batch> store;

    @Inject
    public BlockBatchStore(@NotNull Clans clans) {
        this.store = new ResourceCacheStore<>(new File(clans.getDataFolder(), "scenes/cache/block-batches"),
                new BatchCodec());
    }

    /** Records (overwriting any prior record) the blocks held under {@code id}. */
    public void save(@NotNull UUID id, @NotNull String world, @NotNull List<Schematic.PlacedBlock> blocks) {
        store.put(new Batch(id, world, blocks));
    }

    /** The batch held under {@code id}, or empty if none was recorded. */
    public @NotNull Optional<Batch> get(@NotNull UUID id) {
        return store.get(id.toString());
    }

    /** Drops a batch entirely — used when its owner is permanently removed. */
    public void clear(@NotNull UUID id) {
        store.remove(id.toString());
    }

    /** A batch of blocks keyed by id: its world and the positioned block data to write back. */
    @Value
    public static class Batch {
        UUID id;
        String world;
        List<Schematic.PlacedBlock> blocks;
    }

    /** Serialises a {@link Batch}; blocks whose data no longer parses (a material removed across versions) are dropped. */
    private static final class BatchCodec implements RecordCodec<Batch> {
        @Override
        public @NotNull String key(@NotNull Batch batch) {
            return batch.id.toString();
        }

        @Override
        public void write(@NotNull DataOutputStream out, @NotNull Batch batch) throws IOException {
            out.writeUTF(batch.id.toString());
            out.writeUTF(batch.world);
            out.writeInt(batch.blocks.size());
            for (Schematic.PlacedBlock block : batch.blocks) {
                out.writeInt(block.getX());
                out.writeInt(block.getY());
                out.writeInt(block.getZ());
                out.writeUTF(block.getData().getAsString());
            }
        }

        @Override
        public @NotNull Batch read(@NotNull DataInputStream in) throws IOException {
            final UUID id = UUID.fromString(in.readUTF());
            final String world = in.readUTF();
            final int count = in.readInt();
            final List<Schematic.PlacedBlock> blocks = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                final int x = in.readInt();
                final int y = in.readInt();
                final int z = in.readInt();
                final String data = in.readUTF();
                try {
                    final BlockData blockData = Bukkit.createBlockData(data);
                    blocks.add(new Schematic.PlacedBlock(x, y, z, blockData));
                } catch (IllegalArgumentException invalid) {
                    log.warn("Dropping unparseable block '{}' from block batch {}", data, id).submit();
                }
            }
            return new Batch(id, world, blocks);
        }
    }
}
