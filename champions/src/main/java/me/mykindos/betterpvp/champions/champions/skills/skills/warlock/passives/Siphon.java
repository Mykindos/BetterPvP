package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class Siphon extends Skill implements PassiveSkill, MovementSkill, BuffSkill, HealthSkill, OffensiveSkill {

    private double baseRadius;

    private double radiusIncreasePerLevel;

    private double baseEnergySiphoned;

    private double energySiphonedIncreasePerLevel;

    private int speedStrength;

    @Inject
    public Siphon(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Siphon";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Siphon energy from all enemies within " + getValueString(this::getRadius, level) + " blocks, granting",
                "you <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> and sometimes a small amount of health",
                "",
                "Energy siphoned per second: " + getValueString(this::getEnergySiphoned, level)
        };
    }

    public double getRadius(int level) {
        return baseRadius + ((level - 1) * radiusIncreasePerLevel);
    }

    public double getEnergySiphoned(int level) {
        return baseEnergySiphoned + ((level - 1) * energySiphonedIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @UpdateEvent(delay = 2000)
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level > 0) {
                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, player.getLocation(), getRadius(level))) {
                    if (target instanceof Player playerTarget) {
                        championsManager.getEnergy().degenerateEnergy(playerTarget, ((float) getEnergySiphoned(level)) / 10.0f);
                    }

                    new BukkitRunnable() {
                        private final Location position = target.getLocation().add(0, 1, 0);

                        @Override
                        public void run() {
                            Location playerLoc = player.getLocation().clone().add(0, 1, 0);
                            Vector v = UtilVelocity.getTrajectory(position, playerLoc);
                            if (player.isDead()) {
                                this.cancel();
                                return;
                            }
                            if (position.distance(playerLoc) < 1) {
                                if (UtilMath.randomInt(10) == 1) {
                                    UtilPlayer.health(player, 1);
                                }
                                championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength, 2500, true);
                                this.cancel();
                                return;
                            }

                            Particle.END_ROD.builder().location(position).receivers(30).extra(0).spawn();
                            v.multiply(0.9);
                            position.add(v);
                        }
                    }.runTaskTimer(champions, 0L, 2);
                }
            }

        }
    }


    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 3.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);
        baseEnergySiphoned = getConfig("baseEnergySiphoned", 1.0, Double.class);
        energySiphonedIncreasePerLevel = getConfig("energySiphonedIncreasePerLevel", 0.0, Double.class);

        speedStrength = getConfig("speedStrength", 2, Integer.class);
    }
}
