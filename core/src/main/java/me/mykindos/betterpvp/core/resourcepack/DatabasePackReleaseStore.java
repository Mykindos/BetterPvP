package me.mykindos.betterpvp.core.resourcepack;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.Record;

import java.util.Optional;

/**
 * Reads releases from the resource_pack_release table the pack server writes.
 */
@Singleton
public class DatabasePackReleaseStore implements PackReleaseStore {

    private static final String LATEST =
            "select manifest::text as manifest from resource_pack_release where channel = ? order by id desc limit 1";

    private final Database database;
    private final Gson gson = new Gson();

    @Inject
    public DatabasePackReleaseStore(Database database) {
        this.database = database;
    }

    @Override
    public Optional<PackRelease> latest(String channel) {
        final Record record = database.getDslContext().fetchOne(LATEST, channel);
        if (record == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(gson.fromJson(record.get("manifest", String.class), PackRelease.class));
    }

}
