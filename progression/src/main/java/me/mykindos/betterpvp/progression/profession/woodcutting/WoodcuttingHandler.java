package me.mykindos.betterpvp.progression.profession.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.LongUnaryOperator;


/**
 * This class's purpose is to listen for whenever a block is broken
 * and notify the WoodcuttingHandler appropriately.
 */
@Singleton
@CustomLog
@Getter
public class WoodcuttingHandler extends ProfessionHandler {
    private Map<Material, Long> experiencePerWood = new EnumMap<>(Material.class);

    @Inject
    public WoodcuttingHandler(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression, professionProfileManager, "Woodcutting");
    }

    /**
     * @param material The (type of) wood material that was mined by the player
     * @return The experience gained from mining said wood material
     */
    public long getExperienceFor(Material material) {
        return experiencePerWood.getOrDefault(material, 0L);
    }

    /**
     * Whenever a player mines a block but there is no experience modifier (or whatever
     * is calling this method doesn't know in what manor it needs to modify the xp),
     * it will get passed here
     */
    public void attemptToMineWood(Player player, Block block) {
        attemptToMineWood(player, block, LongUnaryOperator.identity());
    }

    /**
     * Just like the other declaration but this one will take in LongUnaryOperator
     * (which is just a higher order function that returns a long primitive type).
     * @param experienceModifier represents a higher order function that modifies
     *                           the experience gained by the player here.
     */
    public void attemptToMineWood(Player player, Block block, LongUnaryOperator experienceModifier) {
        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) {
            return;
        }

        long experience = getExperienceFor(block.getType());
        if (experience <= 0) {
            return;
        }

        // if this is identity, it will just return 'experience'
        final long finalExperience = experienceModifier.applyAsLong(experience);

        // no xp if a player placed
        final PersistentDataContainer persistentDataContainer = UtilBlock.getPersistentDataContainer(block);
        boolean playerPlaced = persistentDataContainer.has(CoreNamespaceKeys.PLAYER_PLACED_KEY);
        if (playerPlaced) {
            professionData.grantExperience(0, player);
            return;
        }

        professionData.grantExperience(finalExperience, player);

        log.info("{} chopped {} for {} experience", player.getName(), block.getType(), finalExperience)
                .addClientContext(player).addBlockContext(block).addLocationContext(block.getLocation())
                .addContext("Experience", finalExperience + "").submit();

        int treesChopped = (int) professionData.getProperties().getOrDefault("TOTAL_TREES_CHOPPED", 0);
        professionData.getProperties().put("TOTAL_TREES_CHOPPED", treesChopped + 1);
    }

    @Override
    public String getName() {
        return "Woodcutting";
    }

    public void loadConfig() {
        super.loadConfig();

        // not entirely sure if this line is necessary
        experiencePerWood = new EnumMap<>(Material.class);
        var config = progression.getConfig();

        ConfigurationSection experienceSection = config.getConfigurationSection("woodcutting.experiencePerWood");
        if (experienceSection == null) {
            experienceSection = config.createSection("woodcutting.experiencePerWood");
        }

        for (String key : experienceSection.getKeys(false)) {

            Material woodLogMaterial = Material.getMaterial(key.toUpperCase());
            if (woodLogMaterial == null) continue;

            long experienceGiven = config.getLong("woodcutting.experiencePerWood." + key);
            experiencePerWood.put(woodLogMaterial, experienceGiven);
        }
        log.info("Loaded " + experiencePerWood.size() + " woodcutting blocks").submit();
    }
}
