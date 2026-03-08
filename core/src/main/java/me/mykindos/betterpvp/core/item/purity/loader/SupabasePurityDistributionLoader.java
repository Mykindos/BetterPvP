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
import me.mykindos.betterpvp.core.item.purity.distribution.PurityDistribution;
import me.mykindos.betterpvp.core.item.purity.distribution.PurityDistributionDeserializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads purity distributions from Supabase database.
 */
@CustomLog
@Singleton
public class SupabasePurityDistributionLoader {

    private static final int TIMEOUT_SECONDS = 10;
    private String supabaseUrl;
    private String supabaseKey;
    private final Core plugin;
    private final Gson gson;

    @Inject
    public SupabasePurityDistributionLoader(Core plugin) {
        this.plugin = plugin;
        // Initialize Gson with custom deserializer
        this.gson = new GsonBuilder()
                .registerTypeAdapter(PurityDistribution.class, new PurityDistributionDeserializer())
                .create();
    }

    /**
     * Reloads Supabase credentials from config.
     */
    public void reloadCredentials() {
        this.supabaseUrl = plugin.getConfig("config").getString("core.database.supabase.url", "https://abcdefghijklmnop.supabase.co");
        this.supabaseKey = plugin.getConfig("config").getString("core.database.supabase.anon-key", "my-supabase-anon-key");
    }

    /**
     * Loads purity distributions from Supabase.
     *
     * @return Map of distribution name to PurityDistribution
     */
    public Map<String, PurityDistribution> loadDistributions() {
        Map<String, PurityDistribution> distributions = new HashMap<>();

        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseKey == null || supabaseKey.isEmpty()) {
            log.warn("Supabase URL or key is not configured. Skipping purity distribution loading from Supabase.").submit();
            return distributions;
        }

        try {
            // Fetch all purity distributions from Supabase
            String endpoint = supabaseUrl + "/rest/v1/purity_distributions?select=*";
            String jsonResponse = fetchFromSupabase(endpoint);

            if (jsonResponse == null) {
                log.error("Failed to fetch purity distributions from Supabase").submit();
                return distributions;
            }

            // Parse the JSON response
            JsonArray distributionsArray = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (JsonElement element : distributionsArray) {
                try {
                    JsonObject distributionObj = element.getAsJsonObject();

                    // Deserialize the purity distribution
                    PurityDistribution distribution = gson.fromJson(distributionObj, PurityDistribution.class);
                    distributions.put(distribution.getName(), distribution);

                    log.info("Loaded purity distribution: {}", distribution.getName()).submit();
                } catch (Exception e) {
                    log.error("Failed to deserialize purity distribution", e).submit();
                }
            }

            log.info("Successfully loaded {} purity distributions from Supabase", distributions.size()).submit();
        } catch (Exception e) {
            log.error("Error loading purity distributions from Supabase", e).submit();
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
                log.error("HTTP error when fetching from Supabase: {}", responseCode).submit();

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
            log.error("Exception when fetching from Supabase", e).submit();
            return null;
        }
    }
}
