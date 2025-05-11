package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

@CustomLog
public abstract class SingleSimpleAchievementConfigLoader<T extends SingleSimpleAchievement<? extends PropertyContainer, ? extends PropertyUpdateEvent<?>, ? extends Number>> implements IConfigAchievementLoader<T> {

    private final BPvPPlugin plugin;

    public SingleSimpleAchievementConfigLoader(BPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(T achievement) {
        plugin.getListeners().add(achievement);
        Bukkit.getPluginManager().registerEvents(achievement, plugin);
    }

    @Override
    public Collection<T> loadAchievements(ExtendedYamlConfiguration config) {
        String path = getBasePath() + getTypeKey().asString();
        ConfigurationSection section = config.getOrCreateSection(path);
        return section.getKeys(false).stream()
                .map(name -> {
                    Number goal = config.getOrSaveObject(path + "." + name + ".goal", 5, Number.class);
                    T achievement = instanstiateAchievement(NamespacedKey.fromString(name), goal);
                    achievement.loadConfig(path + ".", config);
                    register(achievement);
                    return achievement;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Initialises the {@link IAchievement}, generally by calling {@code new}
     * @param key the {@link NamespacedKey} for this achievement
     * @param goal the goal of the achievement
     * @return the instantiated achievement
     */
    protected abstract T instanstiateAchievement(NamespacedKey key, Number goal);
}
