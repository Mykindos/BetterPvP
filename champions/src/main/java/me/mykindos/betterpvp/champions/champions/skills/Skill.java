package me.mykindos.betterpvp.champions.champions.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.effects.types.SkillBoostEffect;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.effects.Effect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@CustomLog
public abstract class Skill implements IChampionsSkill {

    protected final Champions champions;
    protected final ChampionsManager championsManager;

    private boolean enabled;
    private int maxLevel;
    protected double cooldown;
    protected double cooldownDecreasePerLevel;
    protected int energy;
    protected double energyDecreasePerLevel;
    protected double energyStartCost;
    protected double energyStartCostDecreasePerLevel;

    private boolean canUseWhileSlowed;

    private boolean canUseWhileStunned;

    private boolean canUseWhileSilenced;

    private boolean canUseWhileLevitating;

    private boolean canUseInLiquid;

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
    public boolean canUseWhileSlowed() {
        return canUseWhileSlowed;
    }

    @Override
    public boolean canUseWhileStunned() {
        return canUseWhileStunned;
    }

    @Override
    public boolean canUseWhileSilenced() {
        return canUseWhileSilenced;
    }

    @Override
    public boolean canUseWhileLevitating() {
        return canUseWhileLevitating;
    }

    @Override
    public boolean canUseInLiquid() {
        return canUseInLiquid;
    }

    public Component[] parseDescription(int level) {
        final String[] description = getDescription(level);
        final Component[] components = new Component[description.length];
        for (int i = 0; i < description.length; i++) {
            components[i] = MiniMessage.miniMessage().deserialize("<gray>" + description[i], SkillMenu.TAG_RESOLVER)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return components;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    public void reload() {
        try {
            loadConfig();
        } catch (Exception ex) {
            log.error("Something went wrong loading the skill configuration for {}", getName(), ex).submit();
        }
    }

    private String getPath(String name) {
        String path;
        if (getClassType() != null) {
            path = "skills." + getClassType().name().toLowerCase() + "." + getName().toLowerCase().replace(" ", "") + "." + name;
        } else {
            path = "skills.global." + getName().toLowerCase().replace(" ", "") + "." + name;
        }
        return path;
    }

    protected <T> T getConfig(String name, Object defaultValue, Class<T> type) {
        return champions.getConfig("skills/skills").getOrSaveObject(getPath(name), defaultValue, type);
    }

    /**
     * @param name         name of the value
     * @param defaultValue default value
     * @param type         The type of default value
     * @param <T>          The type of default value
     * @return returns the config value if exists, or the default value if it does not. Does not save value in the config
     */
    protected <T> T getConfigObject(String name, T defaultValue, Class<T> type) {
        return champions.getConfig("skills/skills").getObject(getPath(name), type, defaultValue);
    }


    @Override
    public final void loadConfig() {
        enabled = getConfig("enabled", true, Boolean.class);
        maxLevel = getConfig("maxlevel", 5, Integer.class);

        if (this instanceof CooldownSkill) {
            cooldown = getConfig("cooldown", 1.0, Double.class);
            cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
        }

        if (this instanceof EnergySkill) {
            energy = getConfig("energy", 0, Integer.class);
            energyDecreasePerLevel = getConfig("energyDecreasePerLevel", 1.0, Double.class);
        }

        if (this instanceof ActiveToggleSkill) {
            energyStartCost = getConfig("energyStartCost", 10.0, Double.class);
            energyStartCostDecreasePerLevel = getConfig("energyStartCostDecreasePerLevel", 0.0, Double.class);
        }

        canUseWhileSlowed = getConfigObject("canUseWhileSlowed", true, Boolean.class);
        canUseWhileSilenced = getConfigObject("canUseWhileSilenced", false, Boolean.class);
        canUseWhileStunned = getConfigObject("canUseWhileStunned", false, Boolean.class);
        canUseWhileLevitating = getConfigObject("canUseWhileLevitating", false, Boolean.class);
        canUseInLiquid = getConfigObject("canUseInLiquid", false, Boolean.class);

        loadSkillConfig();
    }

    /**
     * Called when a player, for any reason, equips this skill.
     * i.e: Logging in, changing classes, editing build.
     *
     * @param player The player that equipped the skill
     * @param gamer
     */
    public void trackPlayer(Player player, Gamer gamer) {
    }

    /**
     * Called when a player, for any reason, unequips this skill.
     * i.e: Logging out, changing classes, editing build.
     *
     * @param player The player that unequipped the skill
     * @param gamer
     */
    public void invalidatePlayer(Player player, Gamer gamer) {
    }

    public void loadSkillConfig() {
    }

    public boolean hasSkill(Player player) {
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
        if(level == 0) return 0;

        if (SkillWeapons.isHolding(player, getType()) && SkillWeapons.hasBooster(player)) {
            level++;
        }

        for (Effect effect : championsManager.getEffects().getEffects(player, SkillBoostEffect.class)) {
            if (effect.getEffectType() instanceof SkillBoostEffect skillBoostEffect) {
                if (skillBoostEffect.hasSkillType(getType())) {
                    level += effect.getAmplifier();
                }
            }
        }

        return level;
    }

    @Override
    public boolean isHolding(Player player) {
        return hasSkill(player) && SkillWeapons.isHolding(player, getType());
    }
}
