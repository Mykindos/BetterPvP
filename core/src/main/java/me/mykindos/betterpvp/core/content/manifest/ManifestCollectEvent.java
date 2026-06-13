package me.mykindos.betterpvp.core.content.manifest;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fired on boot to collect the game's manifest (items, zones, npcs, professions,
 * primitives) which is then written to the DB as the single source of truth.
 * Any module contributes by listening and calling the {@code add*} methods —
 * keeping core decoupled from leaf modules (e.g. professions live in progression).
 */
@Getter
public class ManifestCollectEvent extends CustomEvent {

    private final List<Map<String, Object>> items = new ArrayList<>();
    private final List<Map<String, Object>> zones = new ArrayList<>();
    private final List<Map<String, Object>> professions = new ArrayList<>();
    private final List<Map<String, Object>> npcFactories = new ArrayList<>();
    private final List<QuestPrimitive> primitives = new ArrayList<>();

    public void addItem(String key, String displayName, String source, @Nullable String material, List<String> tags) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("display_name", displayName);
        row.put("source", source);
        row.put("material", material);
        row.put("tags", tags);
        items.add(row);
    }

    public void addZone(String key, String displayName, @Nullable String world, List<String> tags) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("display_name", displayName);
        row.put("world", world);
        row.put("tags", tags);
        zones.add(row);
    }

    public void addProfession(String key, String displayName, @Nullable Integer maxLevel) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("display_name", displayName);
        row.put("max_level", maxLevel);
        professions.add(row);
    }

    public void addPrimitive(QuestPrimitive primitive) {
        primitives.add(primitive);
    }

    public void addNpcFactory(String factory, String type) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("factory", factory);
        row.put("type", type);
        npcFactories.add(row);
    }
}
