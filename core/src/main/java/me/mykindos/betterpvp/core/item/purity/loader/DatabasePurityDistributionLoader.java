package me.mykindos.betterpvp.core.item.purity.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.item.purity.distribution.PurityDistribution;
import me.mykindos.betterpvp.core.item.purity.distribution.PurityDistributionDeserializer;
import org.jooq.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads purity distributions from the game PostgreSQL database (replacing the
 * Supabase HTTP loader). Each row is read as a JSON object via
 * {@code row_to_json(...)} so the existing {@link PurityDistributionDeserializer}
 * works unchanged, regardless of the table's column layout.
 */
@CustomLog
@Singleton
public class DatabasePurityDistributionLoader {

    private static final String QUERY = "select published::text as json from purity_distributions where published is not null";

    private final Database database;
    private final Gson gson;

    @Inject
    public DatabasePurityDistributionLoader(Database database) {
        this.database = database;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(PurityDistribution.class, new PurityDistributionDeserializer())
                .create();
    }

    public Map<String, PurityDistribution> loadDistributions() {
        Map<String, PurityDistribution> distributions = new HashMap<>();
        try {
            for (Record record : database.getDslContext().fetch(QUERY)) {
                String json = record.get("json", String.class);
                if (json == null) continue;
                try {
                    PurityDistribution distribution = gson.fromJson(JsonParser.parseString(json).getAsJsonObject(), PurityDistribution.class);
                    distributions.put(distribution.getName(), distribution);
                } catch (Exception ex) {
                    log.error("Failed to deserialize purity distribution", ex).submit();
                }
            }
            log.info("Loaded {} purity distributions from database", distributions.size()).submit();
        } catch (Exception ex) {
            log.error("Error loading purity distributions from database", ex).submit();
        }
        return distributions;
    }
}
