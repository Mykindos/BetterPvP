package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import javax.annotation.Nullable;
import java.util.List;

@Data
@CustomLog
@Singleton
public abstract class ProfessionNode implements IProfessionNode {

    @Inject
    private Progression progression;

    @Inject
    protected ProfessionProfileManager professionProfileManager;

    protected final String name;
    private String displayName;

    private String profession;
    private boolean enabled;
    private int maxLevel;
    private ProfessionNodeDependency dependencies;


    protected ProfessionNode(String name) {
        this.name = name;
    }

    /**
     * Initialize the node after injection
     */
    public void initialize(String profession) {
        this.profession = profession;
        loadConfig();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public abstract Material getIcon();

    public ItemFlag getFlag() {
        return null;
    }

    public boolean isGlowing() {
        return false;
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

    @Override
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

    @Override
    @Nullable
    public ProfessionNodeDependency getDependencies() {
        return dependencies;
    }

}
