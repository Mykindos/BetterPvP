package me.mykindos.betterpvp.progression.profession.farming;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.farming.listener.FarmingActionType;
import me.mykindos.betterpvp.progression.profession.farming.listener.YieldLevel;
import me.mykindos.betterpvp.progression.profession.farming.repository.FarmingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

@Singleton
@CustomLog
@Getter
public class FarmingHandler extends ProfessionHandler {
    private final FarmingRepository farmingRepository;

    /**
     * Maps the log type (key) to its experience value for harvesting it
     */
    private Map<Material, Long> experiencePerCropWhenHarvested;


    public final String LOW_YIELD_METADATA_KEY = "low_yield";


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
    public void attemptToHarvestCrop(Player player, Block harvestedCrop, YieldLevel yieldLevel,
                                     FarmingActionType farmingActionType) {
        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) return;

        Material cropMaterial = harvestedCrop.getType();
        long fullExperience = getExperienceFor(cropMaterial);

        // Probably not a necessary check anymore
        if (fullExperience <= 0) return;

        double finalExperience = 0D;

        switch (yieldLevel) {
            case HIGH -> {
                finalExperience = fullExperience;
                professionData.grantExperience(fullExperience, player);
            }

            // FarmingListener already checks that the crop is ageable when yielding low experience
            case LOW -> {
                Ageable cropAsAgeable = (Ageable) harvestedCrop.getBlockData();
                double lowYieldExperience = ((double) fullExperience) / cropAsAgeable.getMaximumAge();

                finalExperience = lowYieldExperience;
                professionData.grantExperience(lowYieldExperience, player);
            }

            // Essentially just feedback for the player that their action is not rewarded
            case NO_XP -> {
                finalExperience = 0D;  // this line is here for clarity
                professionData.grantExperience(0D, player);
            }
        }

        farmingRepository.saveCropInteraction(player.getUniqueId(), harvestedCrop.getType(), player.getLocation(),
                yieldLevel, farmingActionType);

        log.info("{} {}ED {} for {} experience", player.getName(), farmingActionType.name(), cropMaterial, finalExperience)
                .addClientContext(player).addBlockContext(harvestedCrop).addLocationContext(harvestedCrop.getLocation())
                .addContext("Experience", finalExperience + "").submit();

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
