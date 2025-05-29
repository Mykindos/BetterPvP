package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
public class ProfessionAttributeNode extends ProfessionNode {

    @Getter
    private final Map<ProfessionAttribute, AttributeConfig> attributes = new HashMap<>();

    @Inject
    private ProfessionProfileManager profileManager;

    public ProfessionAttributeNode(String name) {
        super(name);
    }


    @Override
    public Material getIcon() {
        return Material.SNOW;
    }

    @Override
    public String[] getDescription(int level) {
        List<String> desc = new ArrayList<>();
        attributes.forEach((attribute, config) -> {

            desc.add("<green> +" + UtilFormat.formatNumber(config.getBaseValue() + (Math.max(level-1, 0) * config.getPerLevel()), 3) + attribute.getOperation() + "<white> " + attribute.getName());
        });
        return desc.toArray(new String[0]);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();

        attributes.clear();

        List<Map<String, Object>> attributesList = getConfig("attributes", new ArrayList<>(), List.class);

        for (Map<String, Object> attributeConfig : attributesList) {
            try {
                String attributeName = (String) attributeConfig.get("attribute");
                ProfessionAttribute attribute = ProfessionAttribute.valueOf(attributeName);

                double baseValue = ((Number) attributeConfig.getOrDefault("base", 0.0)).doubleValue();
                double perLevel = ((Number) attributeConfig.getOrDefault("per_level", 0.0)).doubleValue();

                attributes.put(attribute, new AttributeConfig(baseValue, perLevel));
            } catch (Exception e) {
                log.error("Error loading attribute config: {}", e.getMessage()).submit();
            }
        }

    }

    /**
     * Get the player's profession profile
     * @param player The player
     * @return The player's profession profile, or null if not found
     */
    protected ProfessionProfile getPlayerProfile(Player player) {
        return profileManager.getObject(player.getUniqueId().toString()).orElse(null);
    }

    /**
     * Get the value of an attribute for a player at their current level
     * 
     * @param player The player
     * @param attribute The attribute to get
     * @return The value of the attribute for the player
     */
    public double getAttributeValue(Player player, ProfessionAttribute attribute) {
        AttributeConfig config = attributes.get(attribute);
        if (config == null) {
            return 0.0;
        }

        ProfessionProfile profile = getPlayerProfile(player);
        if (profile == null) {
            return 0.0;
        }

        int level = getPlayerNodeLevel(profile);
        return config.getBaseValue() + (config.getPerLevel() * level);
    }

    /**
     * Configuration for an attribute
     */
    @Getter
    public static class AttributeConfig {
        private final double baseValue;
        private final double perLevel;

        public AttributeConfig(double baseValue, double perLevel) {
            this.baseValue = baseValue;
            this.perLevel = perLevel;
        }
    }
}
