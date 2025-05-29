package me.mykindos.betterpvp.progression.profession;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public abstract class ProfessionHandler implements IProfession {

    protected final Progression progression;
    protected final ClientManager clientManager;
    protected final ProfessionProfileManager professionProfileManager;
    protected final String profession;

    @Getter
    public boolean enabled;

    @Getter
    public Map<String, Map<String, List<String>>> skillTreeLayout;

    protected ProfessionHandler(Progression progression, ClientManager clientManager, ProfessionProfileManager professionProfileManager, String profession) {
        this.progression = progression;
        this.clientManager = clientManager;
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

        // Load the new nested structure
        ConfigurationSection layoutSection = progression.getConfig("professions/" + profession.toLowerCase() + "/skill_tree")
                .getConfigurationSection("skill_tree.layout");

        skillTreeLayout = new HashMap<>();

        if (layoutSection != null) {
            for (String rowKey : layoutSection.getKeys(false)) {
                ConfigurationSection rowSection = layoutSection.getConfigurationSection(rowKey);
                if (rowSection != null) {
                    Map<String, List<String>> columns = new HashMap<>();
                    for (String colKey : rowSection.getKeys(false)) {
                        List<String> columnItems = rowSection.getStringList(colKey);
                        columns.put(colKey, columnItems);
                    }
                    skillTreeLayout.put(rowKey, columns);
                }
            }
        }


    }

}
