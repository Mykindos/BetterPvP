package me.mykindos.betterpvp.champions.champions.skills.skills.brute.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;
import org.bukkit.Particle;

@Singleton
public class WhirlwindSword extends Skill implements InteractSkill, CooldownSkill {

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
        return "Cyclone";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Pulls all enemies within <val>" + getDistance(level) + "</val> blocks towards you",
                "and deals <val> " + getDamage(level) + "</val> damage",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDistance(int level) {
        return baseDistance + level * distanceIncreasePerLevel;
    }

    public double getDamage(int level){
        return baseDamage + level * damageIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
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
                    // LogManager.addLog(target, player, "Cyclone", 0);
                    UtilVelocity.velocity(target, velocity, 1.2D, false, 0.0D, 0.5D, 4.0D, true);
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
        double spiralDuration = 10;
        Bukkit.getServer().getScheduler().runTaskTimer(champions, new Runnable() {
            double i = 0;
            @Override
            public void run() {
                if (i >= Math.PI * 2) {
                    Bukkit.getScheduler().cancelTasks(champions);
                    return;
                }

                double radius = initialRadius * (1 - (i / (Math.PI * 2)));
                double x = Math.sin(i) * radius;
                double z = Math.cos(i) * radius;

                Location particleLocation = center.clone().add(x, 1, z);
                center.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, particleLocation, 1, 0, 0, 0, 0);

                center.getWorld().playSound(center, Sound.BLOCK_WOOL_STEP, 2f, 1f);

                i += (Math.PI * 2) / spiralDuration;
            }
        }, 0, 1);
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseDistance = getConfig("baseDistance", 4.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 1.0, Double.class);
        baseDamage = getConfig("damage", 3.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
    }
}
