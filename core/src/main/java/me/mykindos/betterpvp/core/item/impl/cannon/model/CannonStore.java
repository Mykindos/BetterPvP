package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.store.RecordCodec;
import me.mykindos.betterpvp.core.framework.store.RecordStore;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Durable home for every player-placed cannon: one small file per cannon, written the moment it changes and deleted
 * the moment it is destroyed.
 * <p>
 * Scene cannons do <b>not</b> go through here - they are declared by the map and rebuilt from it on every load, so a
 * stored copy could outlive an edit to the scene.
 */
@Singleton
public class CannonStore {

    private final RecordStore<CannonRecord> store;

    @Inject
    private CannonStore(@NotNull Core core) {
        this.store = new RecordStore<>(new File(core.getDataFolder(), "/data/cannons"), new Codec());
    }

    public void put(@NotNull CannonRecord record) {
        store.put(record);
    }

    public void remove(@NotNull UUID id) {
        store.remove(id.toString());
    }

    public @NotNull Collection<CannonRecord> all() {
        return store.values();
    }

    private static final class Codec implements RecordCodec<CannonRecord> {

        @Override
        public @NotNull String key(@NotNull CannonRecord record) {
            return record.getId().toString();
        }

        @Override
        public void write(@NotNull DataOutputStream out, @NotNull CannonRecord record) throws IOException {
            out.writeLong(record.getId().getMostSignificantBits());
            out.writeLong(record.getId().getLeastSignificantBits());
            out.writeUTF(record.getWorld());
            out.writeDouble(record.getX());
            out.writeDouble(record.getY());
            out.writeDouble(record.getZ());
            out.writeFloat(record.getYaw());
            out.writeFloat(record.getPitch());
            out.writeUTF(record.getArchetypeId());
            out.writeUTF(record.getPropertiesJson());
            out.writeBoolean(record.getPlacedBy() != null);
            if (record.getPlacedBy() != null) {
                out.writeLong(record.getPlacedBy().getMostSignificantBits());
                out.writeLong(record.getPlacedBy().getLeastSignificantBits());
            }
            out.writeUTF(record.getAmmoId() == null ? "" : record.getAmmoId());
            out.writeDouble(record.getHealth());
            out.writeInt(record.getTags().size());
            for (Map.Entry<String, String> tag : record.getTags().entrySet()) {
                out.writeUTF(tag.getKey());
                out.writeUTF(tag.getValue());
            }
        }

        @Override
        public @NotNull CannonRecord read(@NotNull DataInputStream in) throws IOException {
            final UUID id = new UUID(in.readLong(), in.readLong());
            final String world = in.readUTF();
            final double x = in.readDouble();
            final double y = in.readDouble();
            final double z = in.readDouble();
            final float yaw = in.readFloat();
            final float pitch = in.readFloat();
            final String archetypeId = in.readUTF();
            final String propertiesJson = in.readUTF();
            final UUID placedBy = in.readBoolean() ? new UUID(in.readLong(), in.readLong()) : null;
            final String ammoId = in.readUTF();
            final double health = in.readDouble();
            final Map<String, String> tags = new HashMap<>();
            final int tagCount = in.readInt();
            for (int i = 0; i < tagCount; i++) {
                tags.put(in.readUTF(), in.readUTF());
            }
            return new CannonRecord(id, world, x, y, z, yaw, pitch, archetypeId, propertiesJson, placedBy,
                    ammoId.isEmpty() ? null : ammoId, health, tags);
        }
    }
}
