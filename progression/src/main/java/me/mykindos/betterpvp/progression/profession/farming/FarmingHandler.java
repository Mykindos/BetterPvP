package me.mykindos.betterpvp.progression.profession.farming;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.farming.repository.FarmingActionType;
import me.mykindos.betterpvp.progression.profession.farming.repository.FarmingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

@Singleton
@CustomLog
@Getter
public class FarmingHandler extends ProfessionHandler {
    private final FarmingRepository farmingRepository;

    /**
     * Maps the log type (key) to its experience value for harvesting it
     */
    private Map<Material, Long> experiencePerCropWhenHarvested;


    @Inject
    public FarmingHandler(Progression progression, ProfessionProfileManager professionProfileManager,
                          FarmingRepository farmingRepository) {
        super(progression, professionProfileManager, "Farming");
        this.farmingRepository = farmingRepository;
    }

    /**
     * @param material The type of crop that was interacted with
     * @return The experience gained from harvesting the crop at high yield
     */
    public long getExperienceFor(Material material) {
        return experiencePerCropWhenHarvested.getOrDefault(material, 0L);
    }

    /**
     * This handles all the experience gaining and logging that happens when players harvest crops
     */
    public void attemptToHarvestCrop(Player player, Block harvestedCrop, FarmingActionType farmingActionType) {
        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) return;

        Material cropMaterial = harvestedCrop.getType();
        long experience = getExperienceFor(cropMaterial);

        // Probably not a necessary check anymore
        if (experience <= 0) return;

        switch (farmingActionType) {
            case HARVEST -> {
                if (harvestedCrop.getBlockData() instanceof Ageable cropAsAgeable) {

                    // We don't want to reward players for harvesting early!
                    if (cropAsAgeable.getAge() < cropAsAgeable.getMaximumAge()) {
                        professionData.grantExperience(0, player);
                        return;
                    }

                    player.sendMessage("You Harvested a crop for " + experience + " xp! age: " + cropAsAgeable.getAge());
                    professionData.grantExperience(experience, player);
                } else {
                    player.sendMessage("You mined a block that's not ageable!!");
                    professionData.grantExperience(experience, player);
                }
            }

            case PLANT -> {
                player.sendMessage("PLanted crop and ya get little xp");
            }

            case BONEMEAL -> {
                player.sendMessage("Bonemealed crop and ya get little xp");
            }
        }

    }

    @Override
    public String getName() {
        return "Farming";
    }

    /**
     * Loads the YML configuration for the Farming Profession
     * This function will cause side effects by logging messages to the console
     */
    public void loadConfig() {
        super.loadConfig();

        var config = progression.getConfig();
        ConfigurationSection farmingSection = createOrGetSection(config, "farming");

        experiencePerCropWhenHarvested = new EnumMap<>(Material.class);
        ConfigurationSection experienceSection = createOrGetSection(farmingSection, "experiencePerCropWhenHarvested");

        for (String materialAsKey : experienceSection.getKeys(false)) {

            Material cropMaterial = Material.getMaterial(materialAsKey.toUpperCase());
            if (cropMaterial == null) continue;

            long experienceGiven = experienceSection.getLong(materialAsKey);
            experiencePerCropWhenHarvested.put(cropMaterial, experienceGiven);
        }

        log.info("Loaded " + experiencePerCropWhenHarvested.size() + " farming profession crops").submit();
    }
}
