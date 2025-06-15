package me.mykindos.betterpvp.core.item.config;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemConfig {

    private final BPvPPlugin plugin;
    private final String path;
    private final String configKey;

    private ItemConfig(BPvPPlugin plugin, String path, String configKey) {
        this.plugin = plugin;
        this.path = path;
        this.configKey = configKey;
    }

    public static ItemConfig of(BPvPPlugin plugin, String path, String configKey) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(path, "Path cannot be null");
        Preconditions.checkNotNull(configKey, "Config key cannot be null");
        return new ItemConfig(plugin, path, configKey);
    }

    public static ItemConfig of(BPvPPlugin plugin, BaseItem baseItem) {
        final NamespacedKey key = plugin.getInjector().getInstance(ItemRegistry.class).getKey(baseItem);
        Preconditions.checkNotNull(key, "Item key cannot be null for item: " + baseItem.getClass().getSimpleName());
        return of(plugin, "items/" + baseItem.getItemGroup().name().toLowerCase(), key.getKey());
    }

    public static ItemConfig of(Class<? extends BPvPPlugin> pluginClass, BaseItem baseItem) {
        BPvPPlugin plugin = JavaPlugin.getPlugin(pluginClass);
        return of(plugin, baseItem);
    }

    public static ItemConfig of(BPvPPlugin plugin, String path, BaseItem baseItem) {
        final NamespacedKey key = plugin.getInjector().getInstance(ItemRegistry.class).getKey(baseItem);
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
        return plugin.getConfig(path).getOrSaveObject(getPath(name), defaultValue, type);
    }

    /**
     * @param name         name of the value
     * @param defaultValue default value
     * @param type         The type of default value
     * @param <T>          The type of default value
     * @return returns the config value if exists, or the default value if it does not. Does not save value in the config
     */
    public <T> T getConfigObject(String name, T defaultValue, Class<T> type) {
        return plugin.getConfig(path).getObject(getPath(name), type, defaultValue);
    }

    protected String getPath(String name) {
        return configKey + "." + name;
    }

}
