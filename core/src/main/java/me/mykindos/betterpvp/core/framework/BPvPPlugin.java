package me.mykindos.betterpvp.core.framework;

import com.google.inject.Injector;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.config.WebConfigLoader;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

@CustomLog
public abstract class BPvPPlugin extends JavaPlugin {

    /**
     * Store our own list of listeners as spigot does not register them unless they have an active EventHandler
     */
    @Getter
    private final ArrayList<Listener> listeners;

    private final HashMap<String, ExtendedYamlConfiguration> configs;

    @Getter
    private WebConfigLoader webConfigLoader;

    protected BPvPPlugin() {
        this.listeners = new ArrayList<>();
        this.configs = new HashMap<>();

        File configFile = new File(getDataFolder(), "config.yml");
        if(configFile.exists() && webConfigLoader == null) {
            ExtendedYamlConfiguration config = ExtendedYamlConfiguration.loadConfiguration(configFile);
            String webConfigUrl = config.getString("webconfig.url", "https://betterpvp.net/configs");
            boolean enableWebConfig = config.getBoolean("webconfig.enabled", false);

            if (enableWebConfig && !webConfigUrl.isEmpty()) {
                this.webConfigLoader = new WebConfigLoader(webConfigUrl);
                log.info("Web config loader initialized with URL: {}", webConfigUrl).submit();
            } else {
                log.info("Web config is disabled or URL is not set, using local files only").submit();
                this.webConfigLoader = null;
            }
        }

    }

    public abstract Injector getInjector();

    @Override
    @NotNull
    public ExtendedYamlConfiguration getConfig() {
        return getConfig("config");
    }

    @SneakyThrows
    @NotNull
    public ExtendedYamlConfiguration getConfig(String configName) {
        if (!configs.containsKey(configName)) {
            File configFile = new File(getDataFolder(), configName + ".yml");
            if (!configFile.exists()) {
                saveResource(configName + ".yml", false);
            }

            ExtendedYamlConfiguration config = ExtendedYamlConfiguration.loadConfiguration(configFile);
            if(this.webConfigLoader != null) {
                config = webConfigLoader.loadConfig(getName().toLowerCase() + "/" + configName, configFile);
            }

            configs.put(configName, config);
        }

        return configs.get(configName);
    }

    @Override
    public void reloadConfig() {

        configs.forEach((key, value) -> {
            File configFile = new File(getDataFolder(), key + ".yml");
            ExtendedYamlConfiguration config = ExtendedYamlConfiguration.loadConfiguration(configFile);

            final InputStream defConfigStream = getResource("configs/" + key + ".yml");
            if (defConfigStream == null) {
                return;
            }

            // Load from local file first, then override with web config if available
            if (webConfigLoader != null) {
                config = webConfigLoader.loadConfig(getName().toLowerCase() + "/" + key, configFile);
            }

            configs.put(key, config);
        });

    }

    @Override
    public void saveConfig() {
        configs.forEach((key, value) -> {
            try {
                value.save(new File(getDataFolder(), key + ".yml"));
            } catch (IOException e) {
                log.error("Failed to save config file {}", key, e);
            }
        });
    }

    @Override
    public void saveDefaultConfig() {
        try {
            URI uri = Objects.requireNonNull(getClassLoader().getResource("configs")).toURI();
            try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path myPath = fileSystem.getPath("configs");
                walkAndSaveFiles(myPath);
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Failed to save default config files", e);
        }

    }

    private void walkAndSaveFiles(Path path) {
        try (Stream<Path> paths = Files.walk(path)) {
            paths.forEach(file -> {
                if (Files.isRegularFile(file)) {

                    Path filePath = file.subpath(path.getNameCount(), file.getNameCount());
                    Path targetPath = Paths.get(getDataFolder().getAbsolutePath(), filePath.toString());

                    try {

                        Files.createDirectories(targetPath.getParent());
                        if (!Files.exists(targetPath)) { // Check if the file already exists
                            Files.copy(file, targetPath);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            });
        } catch (IOException e) {
            log.error("Failed to walk and save config files", e);
        }
    }

    public void reload() {
        // Reset web config loader status to try fetching from web again
        if (webConfigLoader != null) {
            webConfigLoader.resetWebConfigStatus();
            log.info("Reset web config loader status, will try to fetch from web again").submit();
        }

        reloadConfig();
        getInjector().getAllBindings().forEach((key, value) -> {
            getInjector().injectMembers(value.getProvider().get());
        });
        listeners.forEach(listener -> {
            getInjector().injectMembers(listener);
        });
    }

}
