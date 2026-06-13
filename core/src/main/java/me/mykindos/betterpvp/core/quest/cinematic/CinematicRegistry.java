package me.mykindos.betterpvp.core.quest.cinematic;

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

/** Holds published cinematics, loaded from the content table. */
@PluginAdapter("Core")
@Singleton
@CustomLog
public class CinematicRegistry implements Reloadable {

    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Map<String, CinematicDefinition> cinematics = new HashMap<>();

    @Inject
    public CinematicRegistry(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public Optional<CinematicDefinition> get(String id) {
        return Optional.ofNullable(cinematics.get(id));
    }

    @Override
    public void reload() {
        cinematics.clear();
        for (ContentRecord record : contentRepository.findPublished(ContentType.CINEMATIC)) {
            if (record.getPublishedJson() == null) continue;
            try {
                CinematicDefinition def = objectMapper.readValue(record.getPublishedJson(), CinematicDefinition.class);
                if (def.getId() == null) def.setId(record.getId());
                cinematics.put(def.getId(), def);
            } catch (Exception ex) {
                log.error("Failed to parse cinematic {}", record.getId(), ex).submit();
            }
        }
        log.info("Loaded {} cinematics from content table", cinematics.size()).submit();
    }
}
