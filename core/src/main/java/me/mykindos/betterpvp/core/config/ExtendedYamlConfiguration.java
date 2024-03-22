package me.mykindos.betterpvp.core.config;

import lombok.CustomLog;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@CustomLog
public class ExtendedYamlConfiguration extends YamlConfiguration {

    @NotNull
    public static ExtendedYamlConfiguration loadConfiguration(@NotNull File file) {
        ExtendedYamlConfiguration config = new ExtendedYamlConfiguration();

        try {
            config.load(file);
        } catch (FileNotFoundException ignored) {
            log.error("Could not find config file {}", file);
        } catch (IOException | InvalidConfigurationException ex) {
            log.error("Cannot load " + file, ex);
        }

        return config;
    }

    public String getOrSaveString(@NotNull String path, String defaultValue) {
        if (isSet(path)) {
            return getString(path);
        } else {
            set(path, defaultValue);
            return defaultValue;
        }
    }

    public int getOrSaveInt(@NotNull String path, int defaultValue) {
        if (isSet(path)) {
            return getInt(path);
        } else {
            set(path, defaultValue);
            return defaultValue;
        }
    }

    public boolean getOrSaveBoolean(@NotNull String path, boolean defaultValue) {
        if (isSet(path)) {
            return getBoolean(path);
        } else {
            set(path, defaultValue);
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getOrSaveObject(@NotNull String path, @NotNull Object defaultValue, Class<T> type) {
        if (!isSet(path)) {
            set(path, defaultValue);
        }

        if(type == List.class) {
            return (T) Objects.requireNonNull(getList(path));
        }

        var result = getObject(path, type);
        return result == null ? (T) defaultValue : result;
    }

}
