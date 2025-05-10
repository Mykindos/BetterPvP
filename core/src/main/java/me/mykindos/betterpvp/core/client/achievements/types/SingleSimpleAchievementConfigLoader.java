package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import org.bukkit.Bukkit;
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
                    T achievement = loadAchievement(path + ".", config, name);
                    register(achievement);
                    return achievement;
                })
                .collect(Collectors.toSet());
    }

    protected abstract T loadAchievement(String basePath, ExtendedYamlConfiguration config, String namespacedKey);
}
