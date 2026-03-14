package me.mykindos.betterpvp.core.tracking.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.tracking.ActivitySnapshot;
import org.jooq.Query;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

@Singleton
@CustomLog
public class ActivitySnapshotRepository {

    private final Database database;

    @Inject
    public ActivitySnapshotRepository(Database database) {
        this.database = database;
    }

    /**
     * Persists all entries in the snapshot as a single batched INSERT.
     * Intended to be called from an async thread — the snapshot is immutable.
     */
    public void persist(ActivitySnapshot snapshot) {
        if (snapshot.entries().isEmpty()) return;

        List<Query> queries = new ArrayList<>(snapshot.entries().size());

        for (ActivitySnapshot.Entry entry : snapshot.entries()) {
            queries.add(database.getDslContext()
                    .insertInto(DSL.table("player_activity_snapshots"),
                            DSL.field("world"),
                            DSL.field("chunk_x"),
                            DSL.field("chunk_z"),
                            DSL.field("heat_value"),
                            DSL.field("peak_heat"),
                            DSL.field("combat_events"),
                            DSL.field("current_players"),
                            DSL.field("recorded_at"))
                    .values(
                            entry.key().world(),
                            entry.key().chunkX(),
                            entry.key().chunkZ(),
                            entry.heatValue(),
                            entry.peakHeat(),
                            entry.combatEvents(),
                            entry.currentPlayers(),
                            snapshot.timestamp()
                    ));
        }

        database.getDslContext().batch(queries).execute();
        log.info("Persisted {} activity snapshot entries at t={}", queries.size(), snapshot.timestamp()).submit();
    }

}
