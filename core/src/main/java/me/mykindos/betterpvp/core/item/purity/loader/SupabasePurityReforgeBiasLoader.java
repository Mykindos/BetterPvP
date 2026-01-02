package me.mykindos.betterpvp.core.item.purity.loader;

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
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBias;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBiasDeserializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads purity reforge bias configurations from Supabase database.
 * <p>
 * Fetches bias data from the `purity_reforge_bias` table which contains
 * beta distribution parameters (alpha, beta) for each purity level.
 * <p>
 * Uses the same Supabase credentials as other loaders:
 * - core.database.supabase.url
 * - core.database.supabase.anon-key
 */
@CustomLog
@Singleton
public class SupabasePurityReforgeBiasLoader {

    private static final int TIMEOUT_SECONDS = 10;
    private String supabaseUrl;
    private String supabaseKey;
    private final Core plugin;
    private final Gson gson;

    @Inject
    public SupabasePurityReforgeBiasLoader(Core plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(PurityReforgeBias.class, new PurityReforgeBiasDeserializer())
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
     * Loads purity reforge biases from Supabase.
     * <p>
     * Fetches all rows from the `purity_reforge_bias` table and deserializes them
     * into PurityReforgeBias objects.
     *
     * @return Map of ItemPurity to PurityReforgeBias, empty if loading fails
     */
    public Map<ItemPurity, PurityReforgeBias> loadBiases() {
        Map<ItemPurity, PurityReforgeBias> biases = new EnumMap<>(ItemPurity.class);

        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseKey == null || supabaseKey.isEmpty()) {
            log.warn("Supabase URL or key is not configured. Skipping purity reforge bias loading from Supabase.").submit();
            return biases;
        }

        try {
            // Fetch all purity reforge biases from Supabase
            String endpoint = supabaseUrl + "/rest/v1/purity_reforge_bias?select=*";
            String jsonResponse = fetchFromSupabase(endpoint);

            if (jsonResponse == null) {
                log.error("Failed to fetch purity reforge biases from Supabase").submit();
                return biases;
            }

            // Parse the JSON response
            JsonArray biasArray = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (JsonElement element : biasArray) {
                try {
                    JsonObject biasObj = element.getAsJsonObject();

                    // Deserialize the bias
                    PurityReforgeBias bias = gson.fromJson(biasObj, PurityReforgeBias.class);
                    biases.put(bias.getPurity(), bias);

                    log.info("Loaded purity reforge bias: {} (α={}, β={})",
                            bias.getPurity(), bias.getAlpha(), bias.getBeta()).submit();
                } catch (Exception e) {
                    log.error("Failed to deserialize purity reforge bias", e).submit();
                }
            }

            log.info("Successfully loaded {} purity reforge biases from Supabase", biases.size()).submit();
        } catch (Exception e) {
            log.error("Error loading purity reforge biases from Supabase", e).submit();
        }

        return biases;
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
                log.error("HTTP error when fetching purity reforge biases from Supabase: {}", responseCode).submit();

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
            log.error("Exception when fetching purity reforge biases from Supabase", e).submit();
            return null;
        }
    }
}
