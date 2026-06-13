package me.mykindos.betterpvp.core.quest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

/**
 * Persists {@link QuestInstance}s and their objective progress to the
 * game-owned {@code quest_instances} / {@code quest_objective_progress} tables.
 */
@Singleton
@CustomLog
public class QuestProgressRepository {

    private final Database database;
    private final Gson gson = new Gson();

    @Inject
    public QuestProgressRepository(Database database) {
        this.database = database;
    }

    /** Upsert an instance (by quest+scope) and replace its objective progress rows. */
    public void save(QuestInstance instance) {
        database.getDslContext().transaction(config -> {
            DSLContext dsl = config.dsl();
            JSONB stages = JSONB.valueOf(gson.toJson(instance.getCurrentStages()));

            Record inserted = dsl.insertInto(table(name("quest_instances")),
                            field(name("id"), UUID.class), field(name("quest_id"), String.class),
                            field(name("scope_type"), String.class), field(name("scope_id"), String.class),
                            field(name("status"), String.class), field(name("current_stages"), JSONB.class))
                    .values(instance.getInstanceId(), instance.getQuestId(),
                            instance.getScopeType(), instance.getScopeId(), instance.getStatus(), stages)
                    .onConflict(field(name("quest_id")), field(name("scope_type")), field(name("scope_id")))
                    .doUpdate()
                    .set(field(name("status"), String.class), instance.getStatus())
                    .set(field(name("current_stages"), JSONB.class), stages)
                    .returning(field(name("id"), UUID.class))
                    .fetchOne();

            UUID storedId = inserted == null
                    ? instance.getInstanceId()
                    : inserted.get(field(name("id"), UUID.class));

            dsl.deleteFrom(table(name("quest_objective_progress")))
                    .where(field(name("instance_id"), UUID.class).eq(storedId))
                    .execute();

            for (var entry : instance.getProgress().entrySet()) {
                dsl.insertInto(table(name("quest_objective_progress")),
                                field(name("instance_id"), UUID.class), field(name("objective_key"), String.class),
                                field(name("progress"), Integer.class), field(name("target"), Integer.class))
                        .values(storedId, entry.getKey(), entry.getValue(),
                                instance.getTargets().getOrDefault(entry.getKey(), 1))
                        .execute();
            }
        });
    }

    /** Load all active instances (used to warm the in-memory cache on startup). */
    public List<QuestInstance> loadAllActive() {
        DSLContext dsl = database.getDslContext();
        List<QuestInstance> instances = new ArrayList<>();

        var rows = dsl.select(
                        field(name("id"), UUID.class), field(name("quest_id"), String.class),
                        field(name("scope_type"), String.class), field(name("scope_id"), String.class),
                        field(name("status"), String.class), field("current_stages::text", String.class).as("stages"))
                .from(table(name("quest_instances")))
                .where(field(name("status"), String.class).eq(QuestInstance.STATUS_ACTIVE))
                .fetch();

        for (Record r : rows) {
            QuestInstance instance = new QuestInstance(
                    r.get("id", UUID.class),
                    r.get("quest_id", String.class),
                    r.get("scope_type", String.class),
                    r.get("scope_id", String.class));
            instance.setStatus(r.get("status", String.class));
            String stagesJson = r.get("stages", String.class);
            if (stagesJson != null) {
                List<String> stages = gson.fromJson(stagesJson, new TypeToken<List<String>>() {}.getType());
                if (stages != null) instance.getCurrentStages().addAll(stages);
            }

            dsl.select(field(name("objective_key"), String.class), field(name("progress"), Integer.class), field(name("target"), Integer.class))
                    .from(table(name("quest_objective_progress")))
                    .where(field(name("instance_id"), UUID.class).eq(instance.getInstanceId()))
                    .fetch()
                    .forEach(o -> {
                        String key = o.get("objective_key", String.class);
                        instance.getProgress().put(key, o.get("progress", Integer.class));
                        instance.getTargets().put(key, o.get("target", Integer.class));
                    });

            instances.add(instance);
        }
        return instances;
    }

    /**
     * Load the {@code scopeType:scopeId:questId} keys of every completed instance, used to
     * warm the in-memory completion gate so finished quests stay finished across restarts.
     * Format mirrors {@code QuestManager#completedKey}.
     */
    public Set<String> loadCompletedScopeKeys() {
        return database.getDslContext()
                .select(field(name("quest_id"), String.class),
                        field(name("scope_type"), String.class), field(name("scope_id"), String.class))
                .from(table(name("quest_instances")))
                .where(field(name("status"), String.class).eq(QuestInstance.STATUS_COMPLETED))
                .fetch()
                .stream()
                .map(r -> r.get("scope_type", String.class) + ":" + r.get("scope_id", String.class)
                        + ":" + r.get("quest_id", String.class))
                .collect(Collectors.toSet());
    }
}
