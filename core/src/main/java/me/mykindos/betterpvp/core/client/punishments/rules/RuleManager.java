package me.mykindos.betterpvp.core.client.punishments.rules;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Set;
@CustomLog
@Singleton
public class RuleManager extends Manager<Rule> {
    public void load(Core core) {
        ExtendedYamlConfiguration config = core.getConfig("rules");
        Set<String> categories = config.getKeys(false);
        log.info("Rule Categories {}", categories.toString()).submit();
        for (String category : categories) {
            ConfigurationSection section = config.getConfigurationSection(category);
            if (section == null) {
                continue;
            }
            Set<String> keys = section.getKeys(false);
            for (String key : keys) {
                String keyValue = config.getString(getPath(category, key, "key"));
                List<String> punishments = config.getStringList(getPath(category, key, "punishment"));
                String description = config.getString(getPath(category, key, "description"));
                log.info("Loading rule: {}", keyValue).submit();
                addObject(keyValue, new Rule(keyValue, punishments, description));
            }
        }
        log.info("Loaded {} Rules", getObjects().size()).submit();
    }

    private String getPath(String category, String key, String value) {
        return category + "." + key + "." + value;
    }
}
