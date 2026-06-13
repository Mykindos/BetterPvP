package me.mykindos.betterpvp.core.item.purity.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBias;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBiasDeserializer;
import org.jooq.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads purity reforge bias configurations from the game PostgreSQL database
 * (replacing the Supabase HTTP loader). Each row is read as a JSON object via
 * {@code row_to_json(...)} and deserialized with the existing
 * {@link PurityReforgeBiasDeserializer}.
 */
@CustomLog
@Singleton
public class DatabasePurityReforgeBiasLoader {

    private static final String QUERY = "select published::text as json from purity_reforge_bias where published is not null";

    private final Database database;
    private final Gson gson;

    @Inject
    public DatabasePurityReforgeBiasLoader(Database database) {
        this.database = database;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(PurityReforgeBias.class, new PurityReforgeBiasDeserializer())
                .create();
    }

    public Map<ItemPurity, PurityReforgeBias> loadBiases() {
        Map<ItemPurity, PurityReforgeBias> biases = new HashMap<>();
        try {
            for (Record record : database.getDslContext().fetch(QUERY)) {
                String json = record.get("json", String.class);
                if (json == null) continue;
                try {
                    PurityReforgeBias bias = gson.fromJson(JsonParser.parseString(json).getAsJsonObject(), PurityReforgeBias.class);
                    biases.put(bias.getPurity(), bias);
                } catch (Exception ex) {
                    log.error("Failed to deserialize purity reforge bias", ex).submit();
                }
            }
            log.info("Loaded {} purity reforge biases from database", biases.size()).submit();
        } catch (Exception ex) {
            log.error("Error loading purity reforge biases from database", ex).submit();
        }
        return biases;
    }
}
