package me.mykindos.betterpvp.progression.profession;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;
import java.util.UUID;

@Singleton
public abstract class ProfessionHandler implements IProfession {

    protected final Progression progression;
    protected final ProfessionProfileManager professionProfileManager;
    protected final String profession;

    @Getter
    public boolean enabled;

    protected ProfessionHandler(Progression progression, ProfessionProfileManager professionProfileManager, String profession) {
        this.progression = progression;
        this.professionProfileManager = professionProfileManager;
        this.profession = profession;
    }

    public ProfessionData getProfessionData(UUID uuid) {
        Optional<ProfessionProfile> professionProfile = professionProfileManager.getObject(uuid.toString());
        if (professionProfile.isPresent()) {
            ProfessionProfile profile = professionProfile.get();
            return profile.getProfessionDataMap().computeIfAbsent(profession, k -> new ProfessionData(uuid, profession));
        }

        return null;
    }

    public void loadConfig() {
        this.enabled = progression.getConfig().getBoolean(profession + ".enabled", true);
    }

    /**
     * This function will try to get a configuration section for the path but if there is none, it will create
     * a new section at path
     */
    public ConfigurationSection createOrGetSection(ConfigurationSection parentSection, String path) {
        ConfigurationSection section = parentSection.getConfigurationSection(path);
        return section != null ? section : parentSection.createSection(path);
    }


    /**
     * Utility method used to determine whether a player placed a <code>block</code>
     * @param block the block in question
     * @return a boolean determining whether the player placed that block
     */
    public boolean didPlayerPlaceBlock(Block block) {
        return UtilBlock.getPersistentDataContainer(block).has(CoreNamespaceKeys.PLAYER_PLACED_KEY);
    }
}
