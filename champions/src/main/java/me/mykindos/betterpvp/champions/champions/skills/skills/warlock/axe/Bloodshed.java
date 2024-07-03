package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
public class Bloodshed extends Skill implements InteractSkill, CooldownSkill, HealthSkill, OffensiveSkill, TeamSkill, BuffSkill {

    private double radius;
    private double radiusIncreasePerLevel;
    private double duration;
    private double durationIncreasePerLevel;
    private int speedStrength;
    private double baseHealthReduction;
    private double healthReductionDecreasePerLevel;

    @Inject
    public Bloodshed(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bloodshed";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sacrifice " + getValueString(this::getHealthReduction, level, 100, "%", 0) + " of your health to give",
                "yourself and all allies within " + getValueString(this::getRadius, level) + " blocks",
                "a surge of speed, granting them <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for " + getValueString(this::getDuration, level) + " seconds.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - ((level -1) * healthReductionDecreasePerLevel);
    }

    public double getDuration(int level) {
        return duration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getRadius(int level) {
        return radius + ((level -1) * radiusIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @Override
    public void activate(Player player,int level) {
        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (player.getHealth() * healthReduction);
        UtilPlayer.slowHealth(champions, player, -proposedHealth, 5, false);

        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, speedStrength, (long) (duration * 1000));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);

        for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), (radius + level))) {
            championsManager.getEffects().addEffect(target, EffectTypes.SPEED, speedStrength, (long) (duration * 1000));
            UtilMessage.simpleMessage(target, getName(), "<yellow>%s</yellow> gave you <white>Speed "+ UtilFormat.getRomanNumeral(speedStrength) + "</white> for <green>%s</green> seconds.", player.getName(), getDuration(level));
            player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);
        }
    }

    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (20 - (20 * healthReduction));

        if (proposedHealth <= 1) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName(), level);
            return false;
        }

        return true;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 5.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);
        duration = getConfig("duration", 9.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
        speedStrength = getConfig("speedStrength", 2, Integer.class);
        baseHealthReduction = getConfig("baseHealthReduction", 0.4, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.05, Double.class);
    }
}
