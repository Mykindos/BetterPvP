package me.mykindos.betterpvp.clans.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.builds.BuildSkill;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfig;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public abstract class Skill implements ISkill {


    protected final Clans clans;
    protected final ChampionsManager championsManager;

    private final SkillConfigFactory configFactory;

    @Getter
    private SkillConfig skillConfig;

    @Inject
    public Skill(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        this.clans = clans;
        this.championsManager = championsManager;
        this.configFactory = configFactory;
        this.skillConfig = configFactory.create(this);
    }

    @Override
    public boolean isEnabled() {
        return skillConfig.isEnabled();
    }

    @Override
    public int getMaxLevel(){
        return skillConfig.getMaxlevel();
    }

    public void reload() {
        this.skillConfig = configFactory.create(this);
    }


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

    protected int getLevel(Player player){
        Optional<BuildSkill> skillOptional = getSkill(player);
        int level = skillOptional.map(BuildSkill::getLevel).orElse(0);
        if(UtilPlayer.isHoldingItem(player, getItemsBySkillType())) {
            if(UtilPlayer.isHoldingItem(player, SkillWeapons.BOOSTERS)){
                level++;
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
