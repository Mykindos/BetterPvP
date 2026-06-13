package me.mykindos.betterpvp.core.quest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.content.ContentRecord;
import me.mykindos.betterpvp.core.content.ContentRepository;
import me.mykindos.betterpvp.core.content.ContentType;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.quest.model.QuestDefinition;
import me.mykindos.betterpvp.core.quest.model.QuestNode;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the published quest definitions, loaded from the {@code content} table
 * (single source of truth). Reloaded by the framework at startup and by the
 * content-publish NOTIFY listener.
 */
@PluginAdapter("Core")
@Singleton
@CustomLog
public class QuestRegistry implements Reloadable {

    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Map<String, QuestDefinition> quests = new HashMap<>();
    /** Trigger type -> quests whose root stage carries an objective of that type. Rebuilt on reload. */
    private final Map<String, List<QuestDefinition>> rootTriggerIndex = new HashMap<>();

    @Inject
    public QuestRegistry(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public Optional<QuestDefinition> get(String id) {
        return Optional.ofNullable(quests.get(id));
    }

    public Map<String, QuestDefinition> getLoaded() {
        return Collections.unmodifiableMap(quests);
    }

    /**
     * Quests that can auto-start from the given trigger type — i.e. their root stage has an
     * objective of that type. Lets the runtime resolve start candidates with one map lookup
     * instead of scanning every definition on each gameplay event.
     */
    public List<QuestDefinition> questsStartableBy(String triggerType) {
        return rootTriggerIndex.getOrDefault(triggerType, Collections.emptyList());
    }

    @Override
    public void reload() {
        quests.clear();
        for (ContentRecord record : contentRepository.findPublished(ContentType.QUEST)) {
            if (record.getPublishedJson() == null) {
                continue;
            }
            try {
                QuestDefinition definition = objectMapper.readValue(record.getPublishedJson(), QuestDefinition.class);
                if (definition.getId() == null) {
                    definition.setId(record.getId());
                }
                quests.put(definition.getId(), definition);
            } catch (Exception ex) {
                log.error("Failed to parse quest {}", record.getId(), ex).submit();
            }
        }
        rebuildRootTriggerIndex();
        log.info("Loaded {} quests from content table", quests.size()).submit();
    }

    /** Index each quest by the trigger types of its root-stage objectives (its possible entry points). */
    private void rebuildRootTriggerIndex() {
        rootTriggerIndex.clear();
        for (QuestDefinition definition : quests.values()) {
            for (QuestNode root : definition.rootStages()) {
                for (PrimitiveData objective : root.getData().getObjectives()) {
                    rootTriggerIndex.computeIfAbsent(objective.getType(), k -> new ArrayList<>()).add(definition);
                }
            }
        }
    }
}
