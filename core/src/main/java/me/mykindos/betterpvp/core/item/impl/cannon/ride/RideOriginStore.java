package me.mykindos.betterpvp.core.item.impl.cannon.ride;

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
import java.util.Optional;
import java.util.UUID;

/**
 * Durable record of where each in-flight rider must be returned to: one file per rider, written the moment they board
 * and deleted the moment they land, so a crash loses at most the ride itself and never the rider's position.
 */
@Singleton
public class RideOriginStore {

    private final RecordStore<RideOrigin> store;

    @Inject
    private RideOriginStore(@NotNull Core core) {
        this.store = new RecordStore<>(new File(core.getDataFolder(), "/data/cannons/rides"), new Codec());
    }

    public void put(@NotNull RideOrigin origin) {
        store.put(origin);
    }

    public @NotNull Optional<RideOrigin> get(@NotNull UUID rider) {
        return store.get(rider.toString());
    }

    public void remove(@NotNull UUID rider) {
        store.remove(rider.toString());
    }

    public @NotNull Collection<RideOrigin> all() {
        return store.values();
    }

    private static final class Codec implements RecordCodec<RideOrigin> {

        @Override
        public @NotNull String key(@NotNull RideOrigin record) {
            return record.getRider().toString();
        }

        @Override
        public void write(@NotNull DataOutputStream out, @NotNull RideOrigin record) throws IOException {
            out.writeLong(record.getRider().getMostSignificantBits());
            out.writeLong(record.getRider().getLeastSignificantBits());
            out.writeUTF(record.getWorld());
            out.writeDouble(record.getX());
            out.writeDouble(record.getY());
            out.writeDouble(record.getZ());
            out.writeFloat(record.getYaw());
            out.writeFloat(record.getPitch());
            out.writeUTF(record.getGameMode());
        }

        @Override
        public @NotNull RideOrigin read(@NotNull DataInputStream in) throws IOException {
            return new RideOrigin(new UUID(in.readLong(), in.readLong()), in.readUTF(), in.readDouble(),
                    in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat(), in.readUTF());
        }
    }
}
