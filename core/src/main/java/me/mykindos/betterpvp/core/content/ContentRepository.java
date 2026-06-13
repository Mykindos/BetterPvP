package me.mykindos.betterpvp.core.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * Read access to authored content from the shared {@code content} table. Only
 * the {@code published} blob is exposed — the game never reads editor drafts.
 * This is the single source of truth for all authored content (loot tables,
 * quests, sagas, conversations, cinematics).
 */
@Singleton
@CustomLog
public class ContentRepository {

    private final Database database;

    @Inject
    public ContentRepository(Database database) {
        this.database = database;
    }

    /** All published content of a given type. */
    public List<ContentRecord> findPublished(ContentType type) {
        var result = database.getDslContext()
                .select(
                        field(name("id"), String.class),
                        field(name("name"), String.class),
                        field(name("version"), Integer.class),
                        field("published::text", String.class).as("published"))
                .from(table(name("content")))
                .where(field(name("type"), String.class).eq(type.getKey()))
                .and(field(name("published")).isNotNull())
                .fetch();

        List<ContentRecord> records = new ArrayList<>();
        for (Record r : result) {
            records.add(new ContentRecord(
                    r.get("id", String.class),
                    type,
                    r.get("name", String.class),
                    r.get("published", String.class),
                    r.get("version", Integer.class) == null ? 0 : r.get("version", Integer.class)));
        }
        return records;
    }

    /** A single published content row by id, if it exists and is published. */
    public Optional<ContentRecord> findById(String id) {
        Record r = database.getDslContext()
                .select(
                        field(name("type"), String.class),
                        field(name("name"), String.class),
                        field(name("version"), Integer.class),
                        field("published::text", String.class).as("published"))
                .from(table(name("content")))
                .where(field(name("id"), String.class).eq(id))
                .and(field(name("published")).isNotNull())
                .fetchOne();
        if (r == null) {
            return Optional.empty();
        }
        return ContentType.fromKey(r.get("type", String.class)).map(type -> new ContentRecord(
                id,
                type,
                r.get("name", String.class),
                r.get("published", String.class),
                r.get("version", Integer.class) == null ? 0 : r.get("version", Integer.class)));
    }
}
