package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Color;

import java.util.*;

@Singleton
@BPvPListener
public class BloodCompass extends Skill implements PassiveSkill {

    private int numArrows;
    private double revealDistance;
    public double pointDistance;
    private Map<UUID, Player> nearestEnemies = new HashMap<>();

    @Inject
    public BloodCompass(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Blood Compass";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Create <val>" + (numArrows + (level-1)) + "</val> blood arrows that will point towards the",
                "location of the nearest enemies within <val>" + (pointDistance * level) +"</val> blocks",
                "",
                "Enemies within <stat>" + revealDistance + "</stat> blocks of you will glow",
        };
    }

    @UpdateEvent(delay = 500)
    public void findEnemies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level <= 0) {
                continue;
            }

            List<Player> enemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), (pointDistance * level));
            enemies.sort(Comparator.comparingDouble(e -> e.getLocation().distance(player.getLocation())));

            nearestEnemies.clear();
            for (int i = 0; i < Math.min(numArrows, enemies.size()); i++) {
                Player enemy = enemies.get(i);
                nearestEnemies.put(enemy.getUniqueId(), enemy);
            }
        }
    }


    @UpdateEvent
    public void drawArrows() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level <= 0) {
                continue;
            }

            nearestEnemies.forEach((uuid, enemy) -> {
                double distanceToEnemy = player.getLocation().distance(enemy.getLocation());
                if (distanceToEnemy <= revealDistance) {
                    UtilPlayer.setGlowing(player, enemy, true);
                } else {
                    drawArrow(player, enemy);
                    UtilPlayer.setGlowing(player, enemy, false);
                }
            });
        }
    }


    private void drawArrow(Player player, Player enemy) {
        Location playerLocation = player.getLocation().add(0, 1, 0); // Raise the start point by 1 block
        Location enemyLocation = enemy.getLocation();

        Vector direction = enemyLocation.toVector().subtract(playerLocation.toVector()).normalize();

        // Reduce the length of the arrow
        double length = 2.0;
        double distance = 0.1;
        double size = 0.5;

        for (double d = 0; d < length; d += distance) {
            Location point = playerLocation.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.REDSTONE, point, 1, new Particle.DustOptions(Color.RED, (float)size));
        }

        // Adjust the arrowhead to be pointed
        double arrowHeadLength = 0.75;
        Vector up = new Vector(0, 1, 0);
        Vector perpendicular = direction.getCrossProduct(up).normalize();
        Vector reverseDirection = direction.clone().multiply(-0.5); // For the pointed arrowhead

        for (double d = 0; d < arrowHeadLength; d += distance) {
            Location side1 = playerLocation.clone().add(direction.clone().multiply(length)).add(perpendicular.clone().multiply(d)).add(reverseDirection.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.REDSTONE, side1, 1, new Particle.DustOptions(Color.RED, (float)size));

            Location side2 = playerLocation.clone().add(direction.clone().multiply(length)).subtract(perpendicular.clone().multiply(d)).add(reverseDirection.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.REDSTONE, side2, 1, new Particle.DustOptions(Color.RED, (float)size));
        }
    }



    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        numArrows = getConfig("numArrows", 1, Integer.class);
        revealDistance = getConfig("revealDistance", 10.0, Double.class);
        pointDistance = getConfig("pointDistance", 32.0, Double.class);
    }
}
