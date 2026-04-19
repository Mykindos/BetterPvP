package me.mykindos.betterpvp.progression.profession;

import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.loader.DrawioNodeLoaderStrategy;
import me.mykindos.betterpvp.progression.profession.skill.loader.NodeLoaderStrategy;
import me.mykindos.betterpvp.progression.profession.skill.loader.YamlNodeLoaderStrategy;
import me.mykindos.betterpvp.progression.profession.skill.tree.SkillTreeLayout;
import me.mykindos.betterpvp.progression.profession.skill.tree.SkillTreeReader;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;
import java.util.UUID;

@Singleton
@CustomLog
public abstract class ProfessionHandler implements IProfession {

    protected final Progression progression;
    protected final ClientManager clientManager;
    protected final ProfessionProfileManager professionProfileManager;
    protected final String profession;

    @Getter
    public boolean enabled;

    @Getter
    private int skillPointInterval = 1;

    @Getter
    @Nullable
    private SkillTreeLayout skillTree;

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

    public NodeLoaderStrategy getNodeLoaderStrategy() {
        File dir = new File(progression.getDataFolder(), "professions/" + profession.toLowerCase());
        for (String name : new String[]{"skill_tree.drawio", "skill_tree.xml"}) {
            File f = new File(dir, name);
            if (f.exists()) return new DrawioNodeLoaderStrategy(progression, f);
        }
        return new YamlNodeLoaderStrategy();
    }

    public void loadConfig() {
        String configPath = profession.toLowerCase();
        this.enabled = progression.getConfig().getBoolean(configPath + ".enabled", true);
        this.skillPointInterval = Math.max(1, progression.getConfig().getOrSaveInt(configPath + ".skillPointInterval", 1));
        loadSkillTree();
    }

    public int getSkillPointsForLevel(int level) {
        return Math.max(0, level / skillPointInterval);
    }

    public int getAvailableSkillPoints(ProfessionData professionData) {
        int currentLevel = professionData.getLevelFromExperience(professionData.getExperience());
        int totalSkillLevels = professionData.getBuild().getNodes().values().stream().mapToInt(Integer::intValue).sum();
        return getSkillPointsForLevel(currentLevel) - totalSkillLevels;
    }

    private void loadSkillTree() {
        File dir = new File(progression.getDataFolder(), "professions/" + profession.toLowerCase());
        File treeFile = resolveTreeFile(dir);

        if (treeFile == null) {
            log.warn("No skill tree file found for profession '{}' in {}", profession, dir.getPath()).submit();
            skillTree = null;
            return;
        }

        try {
            skillTree = SkillTreeReader.forFile(treeFile).read(treeFile);
            log.info("Loaded skill tree for '{}' from {} ({} rows)", profession, treeFile.getName(), skillTree.rowCount()).submit();
        } catch (Exception e) {
            log.error("Failed to load skill tree for profession '{}'", profession, e).submit();
            skillTree = null;
        }
    }

    /**
     * Resolves the skill tree file for this profession.
     * Priority: skill_tree.drawio > skill_tree.xml > skill_tree.yml
     * On first run, copies the bundled default yml to the data folder.
     */
    private File resolveTreeFile(File dir) {
        for (String name : new String[]{"skill_tree.drawio", "skill_tree.xml", "skill_tree.yml"}) {
            File candidate = new File(dir, name);
            if (candidate.exists()) {
                return candidate;
            }
        }

        // Save the bundled default if present (won't overwrite existing files)
        String resourcePath = "configs/professions/" + profession.toLowerCase() + "/skill_tree.yml";
        if (progression.getResource(resourcePath) != null) {
            progression.saveResource(resourcePath, false);
            File saved = new File(progression.getDataFolder(), resourcePath);
            if (saved.exists()) return saved;
        }

        return null;
    }
}
