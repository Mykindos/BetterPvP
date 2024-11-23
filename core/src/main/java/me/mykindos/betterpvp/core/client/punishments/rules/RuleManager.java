package me.mykindos.betterpvp.core.client.punishments.rules;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Set;
@CustomLog
@Singleton
public class RuleManager extends Manager<Rule> {
    public void load(Core core) {
        ExtendedYamlConfiguration config = core.getConfig("rules");

        //used for custom punishments (manual time apply) or punishments with an unknown or removed rule
        addObject("CUSTOM", new Rule("CUSTOM", List.of("MUTE 1 m"), "CUSTOM", List.of("Internal use only"), Material.PAPER, 1));

        Set<String> categories = config.getKeys(false);
        for (String category : categories) {
            ConfigurationSection section = config.getConfigurationSection(category);
            if (section == null) {
                continue;
            }
            Set<String> keys = section.getKeys(false);
            for (String key : keys) {
                if (key.equalsIgnoreCase("custom")) continue;
                String keyValue = config.getString(getPath(category, key, "key"), "");
                List<String> punishments = config.getStringList(getPath(category, key, "punishment"));
                List<String> description = config.getStringList(getPath(category, key, "description"));
                String materialName = config.getString(getPath(category, key, "material"), "DEBUG_STICK");
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    material = Material.DEBUG_STICK;
                }
                int customModelData = config.getInt(getPath(category, key, "customModelData"));

                log.info("Loading rule: {}", keyValue).submit();
                addObject(keyValue.toLowerCase().replace(' ', '_'), new Rule(keyValue, punishments, category, description, material, customModelData));
            }
        }
        log.info("Loaded {} Rules", getObjects().size()).submit();
    }

    public void reload(Core core) {
        this.getObjects().clear();
        this.load(core);
    }

    public Rule getOrCustom(String identifier) {
        return getObject(identifier).orElseGet(() -> getObject("CUSTOM").orElseThrow());
    }

    private String getPath(String category, String key, String value) {
        return category + "." + key + "." + value;
    }
}
