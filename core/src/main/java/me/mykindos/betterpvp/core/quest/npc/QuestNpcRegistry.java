package me.mykindos.betterpvp.core.quest.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.Record;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/** Loads panel-authored quest-giver NPC definitions from the {@code quest_npcs} table. */
@Singleton
@CustomLog
public class QuestNpcRegistry {

    private final Database database;
    private final Map<String, QuestNpcDefinition> npcs = new HashMap<>();

    @Inject
    public QuestNpcRegistry(Database database) {
        this.database = database;
    }

    public Optional<QuestNpcDefinition> get(String id) {
        return Optional.ofNullable(npcs.get(id));
    }

    public void reload() {
        npcs.clear();
        try {
            for (Record r : database.getDslContext().select(
                            field(name("id"), String.class), field(name("display_name"), String.class),
                            field(name("source"), String.class), field(name("factory"), String.class),
                            field(name("type"), String.class), field(name("skin_value"), String.class),
                            field(name("skin_signature"), String.class))
                    .from(table(name("quest_npcs")))
                    .fetch()) {
                String id = r.get("id", String.class);
                npcs.put(id, new QuestNpcDefinition(
                        id,
                        r.get("display_name", String.class),
                        r.get("source", String.class),
                        r.get("factory", String.class),
                        r.get("type", String.class),
                        r.get("skin_value", String.class),
                        r.get("skin_signature", String.class)));
            }
            log.info("Loaded {} quest NPC definitions", npcs.size()).submit();
        } catch (Exception ex) {
            log.error("Failed to load quest NPC definitions", ex).submit();
        }
    }
}
