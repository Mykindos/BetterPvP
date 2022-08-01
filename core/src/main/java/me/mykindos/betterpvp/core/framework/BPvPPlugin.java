package me.mykindos.betterpvp.core.framework;

import com.google.common.base.Charsets;
import com.google.inject.Injector;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public abstract class BPvPPlugin extends JavaPlugin {

    private final File configFile;
    private ExtendedYamlConfiguration config;

    /**
     * Store our own list of listeners as spigot does not register them unless they have an active EventHandler
     */
    @Getter
    private final ArrayList<Object> listeners;

    public BPvPPlugin() {
        this.listeners = new ArrayList<>();
        this.configFile = new File(getDataFolder(), "config.yml");
    }

    public abstract Injector getInjector();


    @Override
    @NotNull
    public ExtendedYamlConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    @Override
    public void reloadConfig() {
        config = ExtendedYamlConfiguration.loadConfiguration(configFile);

        final InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream == null) {
            return;
        }

        config.setDefaults(ExtendedYamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
    }

    @Override
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

}
