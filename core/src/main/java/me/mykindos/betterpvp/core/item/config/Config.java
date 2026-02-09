package me.mykindos.betterpvp.core.item.config;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {

    private final BPvPPlugin plugin;
    private final String path;
    private final String configKey;
    private final String prefix;

    private Config(BPvPPlugin plugin, String path, String configKey) {
        this(plugin, path, configKey, "");
    }

    private Config(BPvPPlugin plugin, String path, String configKey, String prefix) {
        this.plugin = plugin;
        this.path = path;
        this.configKey = configKey;
        this.prefix = prefix;
    }

    /**
     * Fork this config with a prefix.
     * The returned Config will prepend the prefix to all keys.
     *
     * @param section the section prefix (e.g., "dash")
     * @return a new Config scoped to that section
     */
    public Config fork(String section) {
        String newPrefix = prefix.isEmpty()
                ? section + "."
                : prefix + section + ".";
        return new Config(plugin, path, configKey, newPrefix);
    }

    public static Config of(Class<? extends BPvPPlugin> pluginClass, String path, String configKey) {
        BPvPPlugin plugin = JavaPlugin.getPlugin(pluginClass);
        return of(plugin, path, configKey);
    }

    public static Config of(BPvPPlugin plugin, String path, String configKey) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(path, "Path cannot be null");
        Preconditions.checkNotNull(configKey, "Config key cannot be null");
        return new Config(plugin, path, configKey);
    }

    public static Config item(BPvPPlugin plugin, BaseItem baseItem) {
        final ItemKey itemKey = baseItem.getClass().getAnnotation(ItemKey.class);
        Preconditions.checkNotNull(itemKey, "ItemKey annotation cannot be null for item: " + baseItem.getClass().getSimpleName());
        final NamespacedKey key = NamespacedKey.fromString(itemKey.value());
        Preconditions.checkNotNull(key, "Item key cannot be null for item: " + baseItem.getClass().getSimpleName());
        return of(plugin, "items/" + baseItem.getItemGroup().name().toLowerCase(), key.getKey());
    }

    public static Config item(Class<? extends BPvPPlugin> pluginClass, BaseItem baseItem) {
        BPvPPlugin plugin = JavaPlugin.getPlugin(pluginClass);
        return item(plugin, baseItem);
    }

    public static Config item(BPvPPlugin plugin, String path, BaseItem baseItem) {
        final ItemKey itemKey = baseItem.getClass().getAnnotation(ItemKey.class);
        Preconditions.checkNotNull(itemKey, "ItemKey annotation cannot be null for item: " + baseItem.getClass().getSimpleName());
        final NamespacedKey key = NamespacedKey.fromString(itemKey.value());
        Preconditions.checkNotNull(key, "Item key cannot be null for item: " + baseItem.getClass().getSimpleName());
        return of(plugin, path, key.getKey());
    }

    /**
     * @param name         name of the value
     * @param defaultValue default value
     * @param type         The type of default value
     * @param <T>          The type of default value
     * @return returns the config value if exists, or the default value if it does not. Saves the default value if no value exists
     */
    public <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        return plugin.getConfig(path).getOrSaveObject(getPath(prefix + name), defaultValue, type);
    }

    /**
     * @param name         name of the value
     * @param defaultValue default value
     * @param type         The type of default value
     * @param <T>          The type of default value
     * @return returns the config value if exists, or the default value if it does not. Does not save value in the config
     */
    public <T> T getConfigObject(String name, T defaultValue, Class<T> type) {
        return plugin.getConfig(path).getOrSaveObject(getPath(prefix + name), defaultValue, type);
    }

    protected String getPath(String name) {
        return configKey + "." + name;
    }

}
