package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

@Data
@CustomLog
@Singleton
public abstract class ProgressionSkill implements IProgressionSkill {

    private final Progression progression;
    private boolean enabled;
    private int maxLevel;

    protected ProgressionSkill(Progression progression) {
        this.progression = progression;
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

    public void reload() {
        try {
            loadConfig();
        } catch (Exception ex) {
            log.error("Something went wrong loading the skill configuration for {}", getName(), ex).submit();
        }
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        String path = "skills." + getName().toLowerCase().replace(" ", "_") + "." + name;
        return progression.getConfig("professions/" + getProgressionTree().toLowerCase()).getOrSaveObject(path, defaultValue, type);
    }


    @Override
    public void loadConfig() {
        enabled = getConfig("enabled", true, Boolean.class);
        maxLevel = getConfig("maxlevel", 1, Integer.class);
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProgressionSkill pObj) {
            return pObj.getName().equals(getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

}
