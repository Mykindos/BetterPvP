package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

import java.util.Objects;

@Singleton
public abstract class ProfessionSkill implements IProfessionSkill {

    private final String name;

    @Inject
    @Getter
    protected Progression progression;

    @Inject
    protected ProfessionProfileManager profileManager;

    @Inject
    protected Provider<ProfessionNodeManager> nodeManager;

    @Getter
    protected String profession;

    protected ProfessionSkill(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void initialize(String profession) {
        this.profession = profession;
    }

    public int getSkillLevel(Player player) {
        ProfessionProfile profile = profileManager.getObject(player.getUniqueId().toString()).orElse(null);
        if (profile == null) {
            return 0;
        }

        return getSkillLevel(profile);
    }

    public int getSkillLevel(ProfessionProfile profile) {
        if (profile == null || profession == null) {
            return 0;
        }

        var professionData = profile.getProfessionDataMap().get(profession);
        if (professionData == null) {
            return 0;
        }

        return nodeManager.get().getObjects().values().stream()
                .filter(professionNode -> professionNode.getSkill() == this)
                .filter(professionNode -> Objects.equals(professionNode.getProgressionTree(), profession))
                .mapToInt(professionData.getBuild()::getSkillLevel)
                .sum();
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    public String getProgressionTree() {
        return profession;
    }

    protected <T> T getSkillConfig(String key, Object defaultValue, Class<T> type) {
        String professionName = profession.toLowerCase();
        String path = "skills." + getClass().getSimpleName().toLowerCase() + "." + key;
        return progression.getConfig("professions/" + professionName + "/" + professionName).getOrSaveObject(path, defaultValue, type);
    }

    public void loadSkillConfig() {
    }
}
