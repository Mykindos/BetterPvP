package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe.bloodeffects.BloodCircleEffect;
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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

@Singleton
public class Bloodshed extends Skill implements InteractSkill, CooldownSkill, HealthSkill, OffensiveSkill, TeamSkill, BuffSkill {

    private double radius;
    private double radiusIncreasePerLevel;
    private double duration;
    private double durationIncreasePerLevel;
    private int speedStrength;
    private double baseHealthReduction;
    private double healthReductionDecreasePerLevel;
    private double baseHealthReductionPerPlayerAffected;
    private double healthReductionPerPlayerAffectedDecreasePerLevel;

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
                "Sacrifice " + getValueString(this::getHealthReduction, level) + " of your health to give",
                "yourself and all allies within " + getValueString(this::getRadius, level) + " blocks",
                "a surge of speed, granting them <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for " + getValueString(this::getDuration, level) + " seconds.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "Health Sacrifice: " + getValueString(this::getHealthReduction, level, 1) + " + " + getValueString(this::getHealthReductionPerPlayerAffected, level, 1) + " per player affected",

        };
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - ((level -1) * healthReductionDecreasePerLevel);
    }

    public double getHealthReductionPerPlayerAffected(int level) {
        return baseHealthReductionPerPlayerAffected - ((level - 1) * healthReductionPerPlayerAffectedDecreasePerLevel);
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
        double healthReduction = getHealthReduction(level);


        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, speedStrength, (long) (duration * 1000));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);

        for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), (radius + level))) {

            if(player.getHealth() - (healthReduction + getHealthReductionPerPlayerAffected(level)) < 1) {
                break;
            }

            championsManager.getEffects().addEffect(target, EffectTypes.SPEED, speedStrength, (long) (duration * 1000));
            UtilMessage.simpleMessage(target, getName(), "<yellow>%s</yellow> gave you <white>Speed "+ UtilFormat.getRomanNumeral(speedStrength) + "</white> for <green>%s</green> seconds.", player.getName(), getDuration(level));
            player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);
            healthReduction += getHealthReductionPerPlayerAffected(level);

        }
        UtilPlayer.slowHealth(champions, player, -healthReduction, 5, false);

        BloodCircleEffect.runEffect(player.getLocation().add(new Vector(0, 0.1, 0)), getRadius(level), Color.fromRGB(255, 150, 255), Color.fromRGB(255, 100, 100));
        final Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 48);
        new BukkitRunnable() {
            int t = 0;
            double mult = 0.5;
            final Location center = player.getLocation().add(new Vector(0, 0.25, 0));
            @Override
            public void run() {
                for (int i = 0; i < 8; i++) {
                    Location l = center.clone().add(new Vector(getRadius(level) * mult, 0.5d * t, 0).rotateAroundY(Math.toRadians(45d*i + 36d*t)));
                    Particle.SNOWFLAKE.builder()
                            .location(l)
                            .receivers(receivers)
                            .extra(0.0f)
                            .spawn();
                }
                t++;
                if (t > 10)
                    this.cancel();
            }
        }.runTaskTimer(champions, 0, 1);
    }

    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        if (player.getHealth() - getHealthReduction(level) <= 1) {
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
        baseHealthReduction = getConfig("baseHealthReduction", 4.0, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.5, Double.class);
        baseHealthReductionPerPlayerAffected = getConfig("baseHealthReductionPerPlayerAffected", 1.0, Double.class);
        healthReductionPerPlayerAffectedDecreasePerLevel = getConfig("healthReductionPerPlayerAffectedDecreasePerLevel", 0.0, Double.class);
    }
}
