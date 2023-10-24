package me.mykindos.betterpvp.champions.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.components.champions.ISkill;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@Slf4j
public abstract class Skill implements ISkill {


    protected final Champions champions;
    protected final ChampionsManager championsManager;

    private boolean enabled;
    private int maxLevel;
    protected int cooldown;
    protected int energy;

    @Inject
    public Skill(Champions champions, ChampionsManager championsManager) {
        this.champions = champions;
        this.championsManager = championsManager;
        loadConfig();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    public void reload() {
        try {
            loadConfig();
        } catch (Exception ex) {
            log.error("Something went wrong loading the skill configuration for {}", getName(), ex);
        }
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        String path;
        if (getClassType() != null) {
            path = "skills." + getClassType().name().toLowerCase() + "." + getName().toLowerCase().replace(" ", "") + "." + name;
        } else {
            path = "skills.global." + getName().toLowerCase().replace(" ", "") + "." + name;
        }
        return champions.getConfig().getOrSaveObject(path, defaultValue, type);
    }

    @Override
    public final void loadConfig() {
        enabled = getConfig("enabled", true, Boolean.class);
        maxLevel = getConfig("maxlevel", 5, Integer.class);

        if (this instanceof CooldownSkill) {
            cooldown = getConfig("cooldown", 0, Integer.class);
        }

        if (this instanceof EnergySkill) {
            energy = getConfig("energy", 0, Integer.class);
        }

        loadSkillConfig();
    }

    /**
     * Called when a player, for any reason, equips this skill.
     * i.e: Logging in, changing classes, editing build.
     * @param player The player that equipped the skill
     */
    public void trackPlayer(Player player) {
    }

    /**
     * Called when a player, for any reason, unequips this skill.
     * i.e: Logging out, changing classes, editing build.
     * @param player The player that unequipped the skill
     */
    public void invalidatePlayer(Player player) {
    }

    public void loadSkillConfig() {
    }

    protected boolean hasSkill(Player player) {
        Optional<GamerBuilds> gamerBuildsOptional = championsManager.getBuilds().getObject(player.getUniqueId());
        return gamerBuildsOptional.filter(this::hasSkill).isPresent();
    }

    /**
     * Check if a gamer has the current skill equipped
     *
     * @param builds The gamers builds
     * @return True if their
     */
    protected boolean hasSkill(GamerBuilds builds) {
        return getSkill(builds).isPresent();
    }

    protected Optional<BuildSkill> getSkill(Player player) {
        Optional<GamerBuilds> gamerBuildOptional = championsManager.getBuilds().getObject(player.getUniqueId());
        if (gamerBuildOptional.isPresent()) {
            return getSkill(gamerBuildOptional.get());
        }
        return Optional.empty();
    }

    protected Optional<BuildSkill> getSkill(GamerBuilds gamerBuilds) {
        Optional<Role> roleOptional = championsManager.getRoles().getObject(gamerBuilds.getUuid());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            if (role == getClassType() || getClassType() == null) {
                RoleBuild roleBuild = gamerBuilds.getActiveBuilds().get(role.getName());
                BuildSkill buildSkill = roleBuild.getBuildSkill(getType());
                if (buildSkill != null && buildSkill.getSkill() != null) {
                    if (buildSkill.getSkill().equals(this)) {
                        return Optional.of(buildSkill);
                    }
                }
            }
        }

        return Optional.empty();
    }

    protected int getLevel(Player player) {
        Optional<BuildSkill> skillOptional = getSkill(player);
        int level = skillOptional.map(BuildSkill::getLevel).orElse(0);
        if (level > 0) {
            if (UtilPlayer.isHoldingItem(player, getItemsBySkillType())) {
                if (UtilPlayer.isHoldingItem(player, SkillWeapons.BOOSTERS)) {
                    level++;
                }
            }
        }

        return level;
    }

    public Material[] getItemsBySkillType() {
        return switch (getType()) {
            case SWORD -> SkillWeapons.SWORDS;
            case AXE -> SkillWeapons.AXES;
            case BOW -> SkillWeapons.BOWS;
            default -> new Material[]{};
        };
    }

}
