package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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

    @Getter
    private double radius;
    @Getter
    private double duration;

    private int speedStrength;
    @Getter
    private double healthReduction;
    @Getter
    private double healthReductionPerPlayerAffected;

    @Inject
    public Bloodshed(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bloodshed";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sacrifice <val>" + getHealthReduction() + "</val> of your health to give",
                "yourself and all allies within <val>" + getRadius() + "</val> blocks",
                "a surge of speed, granting them <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for <val>" + getDuration() + "</val> seconds.",
                "",
                "Cooldown: <val>" + getCooldown(),
                "Health Sacrifice: <val>" + UtilFormat.formatNumber(getHealthReduction(), 1) + " + <val>" + UtilFormat.formatNumber(getHealthReductionPerPlayerAffected(), 1) + " per player affected",

        };
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
    public void activate(Player player) {
        double healthReduction = getHealthReduction();


        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, speedStrength, (long) (duration * 1000));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);

        for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), radius)) {

            if (player.getHealth() - (healthReduction + getHealthReductionPerPlayerAffected()) < 1) {
                break;
            }

            championsManager.getEffects().addEffect(target, EffectTypes.SPEED, speedStrength, (long) (duration * 1000));
            UtilMessage.simpleMessage(target, getName(), "<yellow>%s</yellow> gave you <white>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</white> for <green>%s</green> seconds.", player.getName(), getDuration());
            player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);
            healthReduction += getHealthReductionPerPlayerAffected();

        }
        UtilPlayer.slowHealth(champions, player, -healthReduction, 5, false);

        BloodCircleEffect.runEffect(player.getLocation().add(new Vector(0, 0.1, 0)), getRadius(), Color.fromRGB(255, 150, 255), Color.fromRGB(255, 100, 100));
        final Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 48);
        new BukkitRunnable() {
            int t = 0;
            double mult = 0.5;
            final Location center = player.getLocation().add(new Vector(0, 0.25, 0));

            @Override
            public void run() {
                for (int i = 0; i < 8; i++) {
                    Location l = center.clone().add(new Vector(getRadius() * mult, 0.5d * t, 0).rotateAroundY(Math.toRadians(45d * i + 36d * t)));
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
        if (player.getHealth() - getHealthReduction() <= 1) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName());
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
        duration = getConfig("duration", 9.0, Double.class);
        speedStrength = getConfig("speedStrength", 2, Integer.class);
        healthReduction = getConfig("healthReduction", 4.0, Double.class);
        healthReductionPerPlayerAffected = getConfig("healthReductionPerPlayerAffected", 1.0, Double.class);
    }
}
