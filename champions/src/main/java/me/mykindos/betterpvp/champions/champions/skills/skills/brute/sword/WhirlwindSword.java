package me.mykindos.betterpvp.champions.champions.skills.skills.brute.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

@Singleton
public class WhirlwindSword extends Skill implements InteractSkill, CooldownSkill, CrowdControlSkill, DamageSkill {

    private double baseDistance;
    private double distanceIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;

    @Inject
    public WhirlwindSword(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Whirlwind Sword";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Pulls all enemies within " + getValueString(this::getDistance, level) + " blocks towards you",
                "and deals " + getValueString(this::getDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getDistance(int level) {
        return baseDistance + (level - 1) * distanceIncreasePerLevel;
    }

    public double getDamage(int level){
        return baseDamage + (level - 1) * damageIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @Override
    public void activate(Player player, int level) {
        Vector vector = player.getLocation().toVector();
        vector.setY(vector.getY() + 2);


        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, player.getLocation(), getDistance(level))) {
            if (!target.getName().equalsIgnoreCase(player.getName())) {
                if (player.hasLineOfSight(target)) {

                    Vector velocity = UtilVelocity.getTrajectory(target, player);
                    VelocityData velocityData = new VelocityData(velocity, 1.0D, true, 0.0D, 0.25D, 4.0D, true);
                    UtilVelocity.velocity(target, player, velocityData);
                    target.damage(getDamage(level));
                    UtilMessage.simpleMessage(target, getName(), "<alt>" + player.getName() + "</alt> hit you with <alt>" + getName());
                }

            }
        }
        createWhirlwind(player, level);
    }

    private void createWhirlwind(Player player, int level) {
        Location center = player.getLocation();
        double initialRadius = getDistance(level);
        int numSpirals = 3;
        int points = 100;
        double spiralDurationTicks = 10;
        double increment = points / spiralDurationTicks;
        int particlesPerTick = 10;

        new BukkitRunnable() {
            double i = 0;
            @Override
            public void run() {
                if (i >= points) {
                    this.cancel();
                    return;
                }

                for (int p = 0; p < particlesPerTick; p++) {
                    double progress = (i + p * increment / particlesPerTick) % points;
                    for (int spiral = 0; spiral < numSpirals; spiral++) {
                        double startAngle = spiral * Math.PI * 2 / numSpirals;
                        double angle = 2 * Math.PI * progress / points + startAngle;

                        double radius = initialRadius * (1 - progress / points);
                        double x = Math.sin(angle) * radius;
                        double z = Math.cos(angle) * radius;

                        Location particleLocation = center.clone().add(x, 1, z);
                        center.getWorld().spawnParticle(Particle.FIREWORK, particleLocation, 1, 0, 0, 0, 0);
                    }
                }

                center.getWorld().playSound(center, Sound.BLOCK_WOOL_STEP, 2f, 1f);
                i += increment;
            }
        }.runTaskTimer(champions, 0, 1);

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseDistance = getConfig("baseDistance", 5.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 0.0, Double.class);
        baseDamage = getConfig("damage", 3.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
    }
}
