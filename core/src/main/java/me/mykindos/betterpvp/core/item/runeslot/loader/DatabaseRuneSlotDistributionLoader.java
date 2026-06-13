package me.mykindos.betterpvp.core.item.runeslot.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.runeslot.RuneSlotDistribution;
import me.mykindos.betterpvp.core.item.runeslot.RuneSlotDistributionDeserializer;
import org.jooq.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads rune-slot distributions from the game PostgreSQL database (replacing the
 * Supabase HTTP loader). Each row is read as a JSON object via
 * {@code row_to_json(...)} and deserialized with the existing
 * {@link RuneSlotDistributionDeserializer}.
 */
@CustomLog
@Singleton
public class DatabaseRuneSlotDistributionLoader {

    private static final String QUERY = "select published::text as json from purity_rune_slot_distributions where published is not null";

    private final Database database;
    private final Gson gson;

    @Inject
    public DatabaseRuneSlotDistributionLoader(Database database) {
        this.database = database;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(RuneSlotDistribution.class, new RuneSlotDistributionDeserializer())
                .create();
    }

    public Map<ItemPurity, RuneSlotDistribution> loadDistributions() {
        Map<ItemPurity, RuneSlotDistribution> distributions = new HashMap<>();
        try {
            for (Record record : database.getDslContext().fetch(QUERY)) {
                String json = record.get("json", String.class);
                if (json == null) continue;
                try {
                    RuneSlotDistribution distribution = gson.fromJson(JsonParser.parseString(json).getAsJsonObject(), RuneSlotDistribution.class);
                    distributions.put(distribution.getPurity(), distribution);
                } catch (Exception ex) {
                    log.error("Failed to deserialize rune slot distribution", ex).submit();
                }
            }
            log.info("Loaded {} rune slot distributions from database", distributions.size()).submit();
        } catch (Exception ex) {
            log.error("Error loading rune slot distributions from database", ex).submit();
        }
        return distributions;
    }
}
