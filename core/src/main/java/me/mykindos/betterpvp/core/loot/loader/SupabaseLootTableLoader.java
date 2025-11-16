package me.mykindos.betterpvp.core.loot.loader;

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
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.serialization.LootTableDeserializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Loads loot tables from a Supabase database.
 */
@CustomLog
@Singleton
public class SupabaseLootTableLoader implements LootTableLoader {

    private static final int TIMEOUT_SECONDS = 10;
    private String supabaseUrl;
    private String supabaseKey;
    private final Core plugin;
    private final Gson gson;

    @Inject
    public SupabaseLootTableLoader(Core plugin) {
        this.plugin = plugin;
        // Initialize Gson with custom deserializer
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LootTable.class, new LootTableDeserializer())
                .create();
    }

    public void reloadCredentials() {
        this.supabaseUrl = plugin.getConfig("config").getString("core.database.supabase.url", "https://abcdefghijklmnop.supabase.co");
        this.supabaseKey = plugin.getConfig("config").getString("core.database.supabase.service-key", "my-supabase-service-key");
    }

    /**
     * Loads loot tables from Supabase.
     *
     * @return The loaded loot tables.
     */
    @Override
    public Collection<LootTable> load() {
        List<LootTable> lootTables = new ArrayList<>();

        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseKey == null || supabaseKey.isEmpty()) {
            log.warn("Supabase URL or key is not configured. Skipping loot table loading from Supabase.").submit();
            return lootTables;
        }

        try {
            // Fetch all loot tables from Supabase
            String endpoint = supabaseUrl + "/rest/v1/loot_tables?select=*";
            String jsonResponse = fetchFromSupabase(endpoint);

            if (jsonResponse == null) {
                log.error("Failed to fetch loot tables from Supabase").submit();
                return lootTables;
            }

            // Parse the JSON response
            JsonArray tablesArray = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (JsonElement element : tablesArray) {
                try {
                    JsonObject tableObj = element.getAsJsonObject();
                    JsonObject definition = tableObj.getAsJsonObject("definition");

                    // Deserialize the loot table definition
                    LootTable lootTable = gson.fromJson(definition, LootTable.class);
                    lootTables.add(lootTable);

                    log.info("Loaded loot table: {}", lootTable.getId()).submit();
                } catch (Exception e) {
                    log.error("Failed to deserialize loot table", e).submit();
                }
            }

            log.info("Successfully loaded {} loot tables from Supabase", lootTables.size()).submit();
        } catch (Exception e) {
            log.error("Error loading loot tables from Supabase", e).submit();
        }

        return lootTables;
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
