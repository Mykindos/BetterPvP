package me.mykindos.betterpvp.core.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.CustomLog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class for loading configuration files from a web server.
 * First loads from local file, then overrides values from web configuration if available.
 * Supports directory structure for different plugins.
 */
@CustomLog
public class WebConfigLoader {

    private static final int TIMEOUT_SECONDS = 5;
    private final String baseUrl;
    private boolean webConfigAvailable = true;

    /**
     * Creates a new WebConfigLoader with the specified base URL.
     *
     * @param baseUrl The base URL to fetch configurations from
     */
    public WebConfigLoader(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    /**
     * Loads a configuration file from local file first, then overrides values from web configuration if available.
     *
     * @param configName The name of the configuration file (without .yml extension)
     * @param localFile  The local file to load first
     * @return The loaded configuration with web overrides if available
     */
    @NotNull
    public ExtendedYamlConfiguration loadConfig(@NotNull String configName, @NotNull File localFile) {
        // Always load from local file first
        ExtendedYamlConfiguration localConfig = ExtendedYamlConfiguration.loadConfiguration(localFile);

        if (!webConfigAvailable) {
            log.info("Web config is not available, using only local file for {}", configName).submit();
            return localConfig;
        }

        try {
            // Try to load from web and override local values
            String configContent = fetchConfigFromWeb(configName);
            if (configContent != null) {
                ExtendedYamlConfiguration webConfig = new ExtendedYamlConfiguration();
                try {
                    webConfig.loadFromString(configContent);

                    // Merge web config into local config (web values override local values)
                    mergeConfigurations(localConfig, webConfig);

                    log.info("Successfully loaded and merged {} configuration from web", configName).submit();
                } catch (InvalidConfigurationException e) {
                    log.error("Invalid configuration format for {} from web, using only local file", configName, e).submit();
                    webConfigAvailable = false;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load {} configuration from web, using only local file", configName, e).submit();
            webConfigAvailable = false;
        }

        return localConfig;
    }

    /**
     * Merges values from web configuration into local configuration.
     * Web values override local values.
     *
     * @param localConfig The local configuration to merge into
     * @param webConfig   The web configuration to merge from
     */
    private void mergeConfigurations(@NotNull ExtendedYamlConfiguration localConfig, @NotNull ExtendedYamlConfiguration webConfig) {
        for (String key : webConfig.getKeys(false)) {
            if (webConfig.isConfigurationSection(key)) {
                ConfigurationSection webSection = webConfig.getConfigurationSection(key);
                if (webSection != null) {
                    mergeConfigurationSection(localConfig, webSection, key);
                }
            } else {
                // Override the value in local config with the value from web config
                localConfig.set(key, webConfig.get(key));
            }
        }
    }

    /**
     * Recursively merges a configuration section from web configuration into local configuration.
     *
     * @param localConfig The local configuration to merge into
     * @param webSection  The web configuration section to merge from
     * @param path        The path of the configuration section
     */
    private void mergeConfigurationSection(@NotNull ExtendedYamlConfiguration localConfig, 
                                          @NotNull ConfigurationSection webSection, 
                                          @NotNull String path) {
        for (String key : webSection.getKeys(false)) {
            String fullPath = path + "." + key;
            if (webSection.isConfigurationSection(key)) {
                ConfigurationSection nestedWebSection = webSection.getConfigurationSection(key);
                if (nestedWebSection != null) {
                    mergeConfigurationSection(localConfig, nestedWebSection, fullPath);
                }
            } else {
                // Override the value in local config with the value from web config
                localConfig.set(fullPath, webSection.get(key));
            }
        }
    }

    /**
     * Fetches a configuration file from the web server.
     * Supports directory structure for different plugins.
     *
     * @param configName The name of the configuration file (without .yml extension)
     * @return The content of the configuration file, or null if the web server is unreachable
     */
    @Nullable
    private String fetchConfigFromWeb(@NotNull String configName) {
        // Support directory structure (e.g., core/resourcepacks.yml, champions/skills/skills.yml)
        String configUrl = baseUrl + configName.replace('\\', '/') + ".yml";

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(configUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_SECONDS * 1000);
                connection.setReadTimeout(TIMEOUT_SECONDS * 1000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        return content.toString();
                    }
                } else {
                    log.error("HTTP error when fetching config {}: {}", configName, responseCode).submit();
                    return null;
                }
            } catch (IOException e) {
                log.error("IO error when fetching config {}", configName, e).submit();
                return null;
            }
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error or timeout when fetching config {}", configName, e).submit();
            future.cancel(true);
            return null;
        }
    }

    /**
     * Checks if web configuration is available.
     *
     * @return true if web configuration is available, false otherwise
     */
    public boolean isWebConfigAvailable() {
        return webConfigAvailable;
    }

    /**
     * Resets the web configuration availability status.
     * This can be used to retry web configuration loading after a failure.
     */
    public void resetWebConfigStatus() {
        this.webConfigAvailable = true;
    }
}
