package me.mykindos.betterpvp.clans.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.builds.BuildSkill;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@Slf4j
public abstract class Skill implements ISkill {


    protected final Clans clans;
    protected final ChampionsManager championsManager;

    private boolean enabled;
    private int maxLevel;
    protected int cooldown;
    protected int energy;
    
    @Inject
    public Skill(Clans clans, ChampionsManager championsManager) {
        this.clans = clans;
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
        }catch(Exception ex){
            log.error("Something went wrong loading the skill configuration for {}", getName(), ex);
        }
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        String path = "skills." + getClassType().name().toLowerCase() + "." + getName().toLowerCase().replace(" ", "") + "." + name;
        return clans.getConfig().getOrSaveObject(path, defaultValue, type);
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

    public void loadSkillConfig(){}

    protected boolean hasSkill(Player player) {
        Optional<Gamer> gamerOptional = championsManager.getGamers().getObject(player.getUniqueId());
        return gamerOptional.filter(this::hasSkill).isPresent();
    }

    /**
     * Check if a gamer has the current skill equipped
     *
     * @param gamer The gamer
     * @return True if their
     */
    protected boolean hasSkill(Gamer gamer) {
        return getSkill(gamer).isPresent();
    }

    protected Optional<BuildSkill> getSkill(Player player) {
        Optional<Gamer> gamerOptional = championsManager.getGamers().getObject(player.getUniqueId());
        if (gamerOptional.isPresent()) {
            return getSkill(gamerOptional.get());
        }
        return Optional.empty();
    }

    protected Optional<BuildSkill> getSkill(Gamer gamer) {
        Optional<Role> roleOptional = championsManager.getRoles().getObject(gamer.getUuid());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            if (role == getClassType()) {
                RoleBuild roleBuild = gamer.getActiveBuilds().get(role.getName());
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

    protected Material[] getItemsBySkillType() {
        return switch (getType()) {
            case SWORD -> SkillWeapons.SWORDS;
            case AXE -> SkillWeapons.AXES;
            case BOW -> SkillWeapons.BOWS;
            default -> new Material[]{};
        };
    }
}
