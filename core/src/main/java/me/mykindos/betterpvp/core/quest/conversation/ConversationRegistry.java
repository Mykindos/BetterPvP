package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.content.ContentRecord;
import me.mykindos.betterpvp.core.content.ContentRepository;
import me.mykindos.betterpvp.core.content.ContentType;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Holds published conversations, loaded from the content table. */
@PluginAdapter("Core")
@Singleton
@CustomLog
public class ConversationRegistry implements Reloadable {

    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Map<String, ConversationDefinition> conversations = new HashMap<>();

    @Inject
    public ConversationRegistry(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public Optional<ConversationDefinition> get(String id) {
        return Optional.ofNullable(conversations.get(id));
    }

    @Override
    public void reload() {
        conversations.clear();
        for (ContentRecord record : contentRepository.findPublished(ContentType.CONVERSATION)) {
            if (record.getPublishedJson() == null) continue;
            try {
                ConversationDefinition def = objectMapper.readValue(record.getPublishedJson(), ConversationDefinition.class);
                if (def.getId() == null) def.setId(record.getId());
                conversations.put(def.getId(), def);
            } catch (Exception ex) {
                log.error("Failed to parse conversation {}", record.getId(), ex).submit();
            }
        }
        log.info("Loaded {} conversations from content table", conversations.size()).submit();
    }
}
