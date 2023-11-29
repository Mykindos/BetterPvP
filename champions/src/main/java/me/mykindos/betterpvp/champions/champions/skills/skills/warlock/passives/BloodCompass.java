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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
                "Create <val>" + (numArrows + (level-1)) + "</val> blood arrows that will",
                "point towards the location of the nearest enemies",
                "",
                "When you get within <stat>" + revealDistance + "</stat> blocks of a tracked enemy,",
                "they will begin glowing"
        };
    }

    @UpdateEvent(delay = 1000)
    public void findEnemies(Player player) {
        if (player == null || !player.isOnline()) {
            nearestEnemies.clear();
            return;
        }

        List<Player> enemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), pointDistance);
        enemies.sort(Comparator.comparingDouble(e -> e.getLocation().distance(player.getLocation())));

        nearestEnemies.clear();
        for (int i = 0; i < Math.min(numArrows, enemies.size()); i++) {
            Player enemy = enemies.get(i);
            nearestEnemies.put(enemy.getUniqueId(), enemy);
        }
    }

    @UpdateEvent
    public void drawArrows(Player player) {
        if (player == null) return;

        nearestEnemies.forEach((uuid, enemy) -> {
            if (player.getLocation().distance(enemy.getLocation()) <= revealDistance) {
                UtilPlayer.setGlowing(player, enemy, true);            }
            drawArrow(player, enemy);
        });
    }

    private void drawArrow(Player player, Player enemy) {
        Location playerLocation = player.getLocation();
        Location enemyLocation = enemy.getLocation();

        Vector direction = enemyLocation.toVector().subtract(playerLocation.toVector()).normalize();

        //length of line and distance between particles
        double length = 5.0;
        double distance = 0.1;

        for (double d = 0; d < length; d += distance) {
            Location point = playerLocation.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, point, 1);
        }

        // draws arrowhead(hopefully)
        double arrowHeadLength = 1.0;
        Vector perpendicular = new Vector(-direction.getZ(), direction.getY(), direction.getX()).normalize();
        for (double d = 0; d < arrowHeadLength; d += distance) {
            Location side1 = playerLocation.clone().add(direction.clone().multiply(length)).add(perpendicular.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, side1, 1);

            Location side2 = playerLocation.clone().add(direction.clone().multiply(length)).subtract(perpendicular.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, side2, 1);
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
        pointDistance = getConfig("pointDistance", 64.0, Double.class);
    }
}