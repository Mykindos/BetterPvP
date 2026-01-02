package me.mykindos.betterpvp.core.item.runeslot.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.runeslot.RuneSlotDistribution;
import me.mykindos.betterpvp.core.item.runeslot.RuneSlotDistributionDeserializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads rune slot distributions from Supabase database.
 * <p>
 * Fetches distribution data from the `purity_rune_slot_distributions` table which contains
 * weight maps for rolling sockets and maxSockets for each purity level.
 * <p>
 * Uses the same Supabase credentials as other loaders:
 * <ul>
 *     <li>core.database.supabase.url</li>
 *     <li>core.database.supabase.anon-key</li>
 * </ul>
 */
@CustomLog
@Singleton
public class SupabaseRuneSlotDistributionLoader {

    private static final int TIMEOUT_SECONDS = 10;
    private String supabaseUrl;
    private String supabaseKey;
    private final Core plugin;
    private final Gson gson;

    @Inject
    public SupabaseRuneSlotDistributionLoader(Core plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(RuneSlotDistribution.class, new RuneSlotDistributionDeserializer())
                .create();
    }

    /**
     * Reloads Supabase credentials from plugin configuration.
     */
    public void reloadCredentials() {
        this.supabaseUrl = plugin.getConfig("config").getString(
                "core.database.supabase.url",
                "https://abcdefghijklmnop.supabase.co"
        );
        this.supabaseKey = plugin.getConfig("config").getString(
                "core.database.supabase.anon-key",
                "my-supabase-anon-key"
        );
    }

    /**
     * Loads rune slot distributions from Supabase.
     * <p>
     * Fetches all rows from the `purity_rune_slot_distributions` table and deserializes them
     * into RuneSlotDistribution objects.
     *
     * @return Map of ItemPurity to RuneSlotDistribution, empty if loading fails
     */
    public Map<ItemPurity, RuneSlotDistribution> loadDistributions() {
        Map<ItemPurity, RuneSlotDistribution> distributions = new EnumMap<>(ItemPurity.class);

        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseKey == null || supabaseKey.isEmpty()) {
            log.warn("Supabase URL or key is not configured. Skipping rune slot distribution loading from Supabase.").submit();
            return distributions;
        }

        try {
            // Fetch all rune slot distributions from Supabase
            String endpoint = supabaseUrl + "/rest/v1/purity_rune_slot_distributions?select=*";
            String jsonResponse = fetchFromSupabase(endpoint);

            if (jsonResponse == null) {
                log.error("Failed to fetch rune slot distributions from Supabase").submit();
                return distributions;
            }

            // Parse the JSON response
            JsonArray distArray = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (JsonElement element : distArray) {
                try {
                    JsonObject distObj = element.getAsJsonObject();

                    // Deserialize the distribution
                    RuneSlotDistribution distribution = gson.fromJson(distObj, RuneSlotDistribution.class);
                    distributions.put(distribution.getPurity(), distribution);

                    log.info("Loaded rune slot distribution: {} (sockets={}, maxSockets={})",
                            distribution.getPurity(),
                            distribution.getSocketWeights(),
                            distribution.getMaxSocketWeights()).submit();
                } catch (Exception e) {
                    log.error("Failed to deserialize rune slot distribution", e).submit();
                }
            }

            log.info("Successfully loaded {} rune slot distributions from Supabase", distributions.size()).submit();
        } catch (Exception e) {
            log.error("Error loading rune slot distributions from Supabase", e).submit();
        }

        return distributions;
    }

    /**
     * Fetches data from a Supabase endpoint.
     *
     * @param endpoint The full endpoint URL
     * @return The JSON response as a string, or null if the request failed
     */
    private String fetchFromSupabase(String endpoint) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_SECONDS * 1000);
            connection.setReadTimeout(TIMEOUT_SECONDS * 1000);

            // Set Supabase headers
            connection.setRequestProperty("apikey", supabaseKey);
            connection.setRequestProperty("Authorization", "Bearer " + supabaseKey);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                    return content.toString();
                }
            } else {
                log.error("HTTP error when fetching rune slot distributions from Supabase: {}", responseCode).submit();

                // Try to read error response
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorContent = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorContent.append(line);
                    }
                    log.error("Error response: {}", errorContent.toString()).submit();
                } catch (Exception e) {
                    // Ignore error reading error response
                }

                return null;
            }
        } catch (Exception e) {
            log.error("Exception when fetching rune slot distributions from Supabase", e).submit();
            return null;
        }
    }
}
