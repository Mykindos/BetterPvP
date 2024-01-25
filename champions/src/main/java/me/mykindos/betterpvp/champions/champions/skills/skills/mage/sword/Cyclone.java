package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

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
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;
import org.bukkit.Particle;

@Singleton
public class Cyclone extends Skill implements InteractSkill, CooldownSkill {

    private double baseDistance;

    private double distanceIncreasePerLevel;

    @Inject
    public Cyclone(Champions champions, ChampionsManager championsManager) {
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
                "Pulls all enemies within",
                "<val>" + getDistance(level) + "</val> blocks towards you",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDistance(int level) {
        return baseDistance + level * distanceIncreasePerLevel;
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
                    UtilVelocity.velocity(target, velocity, 1.2D, false, 0.0D, 0.5D, 4.0D, true, true);
                }

            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.6F);
        createCyclone(player, level);
    }

    private void createCyclone(Player player, int level) {
        Location center = player.getLocation();
        double radius = getDistance(level);
        int points = 100;
        double height = center.getY() + 1.0;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;

            for (int j = 0; j < 4; j++) {
                double startAngle = j * Math.PI / 2;

                double x = center.getX() + radius * Math.cos(angle + startAngle) * ((double) i / points);
                double z = center.getZ() + radius * Math.sin(angle + startAngle) * ((double) i / points);

                Location particleLocation = new Location(center.getWorld(), x, height, z);
                center.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, particleLocation, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseDistance = getConfig("baseDistance", 7.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 1.0, Double.class);
    }
}
