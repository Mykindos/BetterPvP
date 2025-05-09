package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.Collection;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;
import org.bukkit.configuration.ConfigurationSection;

@CustomLog
public abstract class SingleSimpleAchievementConfigLoader<T extends SingleSimpleAchievement<? extends PropertyContainer, ? extends PropertyUpdateEvent<?>, ? extends Number>> implements IConfigAchievementLoader<T> {
    @Override
    public Collection<T> loadAchievements(ExtendedYamlConfiguration config) {
        String path = getBasePath() + getTypeKey().asString();
        ConfigurationSection section = config.getOrCreateSection(path);
        log.error(section.getValues(true).toString()).submit();
        return Set.of();
    }

    /*protected T loadAchievement(String key) {

    }*/
}
