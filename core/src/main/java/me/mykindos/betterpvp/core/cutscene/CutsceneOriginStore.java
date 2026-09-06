package me.mykindos.betterpvp.core.cutscene;

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
import java.util.Optional;
import java.util.UUID;

/**
 * Durable record of where each viewer must be returned to: one file per viewer, written the moment the camera is
 * taken and deleted the moment it is given back, so a crash costs the cutscene and never the player's position.
 */
@Singleton
public class CutsceneOriginStore {

    private final RecordStore<CutsceneOrigin> store;

    @Inject
    private CutsceneOriginStore(@NotNull Core core) {
        this.store = new RecordStore<>(new File(core.getDataFolder(), "/data/cutscenes/origins"), new Codec());
    }

    public void put(@NotNull CutsceneOrigin origin) {
        store.put(origin);
    }

    public @NotNull Optional<CutsceneOrigin> get(@NotNull UUID viewer) {
        return store.get(viewer.toString());
    }

    public void remove(@NotNull UUID viewer) {
        store.remove(viewer.toString());
    }

    private static final class Codec implements RecordCodec<CutsceneOrigin> {

        @Override
        public @NotNull String key(@NotNull CutsceneOrigin record) {
            return record.getViewer().toString();
        }

        @Override
        public void write(@NotNull DataOutputStream out, @NotNull CutsceneOrigin record) throws IOException {
            out.writeLong(record.getViewer().getMostSignificantBits());
            out.writeLong(record.getViewer().getLeastSignificantBits());
            out.writeUTF(record.getCutscene());
            out.writeUTF(record.getWorld());
            out.writeDouble(record.getX());
            out.writeDouble(record.getY());
            out.writeDouble(record.getZ());
            out.writeFloat(record.getYaw());
            out.writeFloat(record.getPitch());
            out.writeUTF(record.getGameMode());
        }

        @Override
        public @NotNull CutsceneOrigin read(@NotNull DataInputStream in) throws IOException {
            return new CutsceneOrigin(new UUID(in.readLong(), in.readLong()), in.readUTF(), in.readUTF(),
                    in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat(), in.readUTF());
        }
    }
}
