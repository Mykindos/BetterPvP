package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.core.skill.ISkill;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import javax.annotation.Nullable;
import java.util.List;

@Data
@CustomLog
public abstract class ProfessionNode implements ISkill {

    @Inject
    private Progression progression;

    @Inject
    protected ProfessionProfileManager professionProfileManager;

    protected final String name;
    protected ProfessionSkill skill;
    private String displayName;

    private String profession;
    private boolean enabled;
    private int maxLevel;
    private ProfessionNodeDependency dependencies;

    protected boolean dataInitialized = false;

    protected ProfessionNode(String name) {
        this.name = name;
    }

    /**
     * Initialize the node after injection.
     * If {@code dataInitialized} is true the node was constructed with inline data
     * and {@link #loadConfig()} is skipped.
     */
    public void initialize(String profession) {
        this.profession = profession;
        if (!dataInitialized) {
            loadConfig();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Material getIcon() {
        return skill == null ? Material.BARRIER : skill.getIcon();
    }

    @Override
    public String[] getDescription(int level) {
        return skill == null ? new String[0] : skill.getDescription(level);
    }

    public ItemFlag getFlag() {
        return skill == null ? null : skill.getFlag();
    }

    public boolean isGlowing() {
        return skill != null && skill.isGlowing();
    }

    public void reload() {
        try {
            loadConfig();
        } catch (Exception ex) {
            log.error("Something went wrong loading the skill configuration for {}", getName(), ex).submit();
        }
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        String path = "nodes." + getName().toLowerCase().replace(" ", "_") + "." + name;
        String professionName = getProgressionTree().toLowerCase();
        return progression.getConfig("professions/" + professionName + "/" + professionName).getOrSaveObject(path, defaultValue, type);
    }

    protected List<String> getStringList(String name) {
        String path = "nodes." + getName().toLowerCase().replace(" ", "_") + "." + name;
        String professionName = getProgressionTree().toLowerCase();
        return progression.getConfig("professions/" + professionName + "/" + professionName).getStringList(path);
    }


    @Override
    public void loadConfig() {
        enabled = getConfig("enabled", true, Boolean.class);
        maxLevel = getConfig("maxlevel", 1, Integer.class);

        displayName = getConfig("displayName", getName(), String.class);

        // Load dependencies from nested config structure
        List<String> dependencyNodes = getStringList("dependencies.nodes");
        int levelsRequired = getConfig("dependencies.levelsRequired", 0, Integer.class);
        int requiredLevel = getConfig("dependencies.requiredLevel", 0, Integer.class);

        dependencies = new ProfessionNodeDependency(dependencyNodes, levelsRequired, requiredLevel);

    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    public String getProgressionTree() {
        return profession;
    }

    public int getPlayerNodeLevel(ProfessionProfile profile) {
        var professionData = profile.getProfessionDataMap().get(profession);
        if (professionData == null) return 0;

        return professionData.getBuild().getSkillLevel(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProfessionNode pObj) {
            return pObj.getName().equals(getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Nullable
    public ProfessionNodeDependency getDependencies() {
        return dependencies;
    }

}
