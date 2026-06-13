package me.mykindos.betterpvp.core.loot.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.content.ContentRecord;
import me.mykindos.betterpvp.core.content.ContentRepository;
import me.mykindos.betterpvp.core.content.ContentType;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.serialization.LootTableDeserializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Loads loot tables from the shared {@code content} table (the single source of
 * truth authored in the admin console). Replaces {@link SupabaseLootTableLoader}
 * — the loot JSON shape is identical, so the existing {@link LootTableDeserializer}
 * is reused unchanged; only the data source moves from Supabase to the game DB.
 */
@Singleton
@CustomLog
public class ContentLootTableLoader implements LootTableLoader {

    private final ContentRepository contentRepository;
    private final Gson gson;

    @Inject
    public ContentLootTableLoader(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LootTable.class, new LootTableDeserializer())
                .create();
    }

    @Override
    public Collection<LootTable> load() {
        List<LootTable> tables = new ArrayList<>();
        for (ContentRecord record : contentRepository.findPublished(ContentType.LOOT_TABLE)) {
            if (record.getPublishedJson() == null) {
                continue;
            }
            try {
                tables.add(gson.fromJson(record.getPublishedJson(), LootTable.class));
            } catch (Exception ex) {
                log.error("Failed to deserialize loot table {}", record.getId(), ex).submit();
            }
        }
        log.info("Loaded {} loot tables from content table", tables.size()).submit();
        return tables;
    }
}
