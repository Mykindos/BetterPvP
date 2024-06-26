package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class BloodCompass extends Skill implements CooldownToggleSkill, Listener, DebuffSkill {

    private int numLines;
    private double maxDistance;
    private int effectDuration;
    private int numPoints;
    private double escapeRadius;
    private double escapeRadiusIncreasePerLevel;
    private double maxDistanceIncreasePerLevel;
    private int effectDurationIncreasePerLevel;

    private final Map<Player, List<List<Location>>> playerMarkersMap = new WeakHashMap<>();
    private final Map<Player, WeakHashMap<Integer, Player>> playerLineToPlayerMap = new WeakHashMap<>();
    private final Map<Player, Integer> playerTaskIdMap = new WeakHashMap<>();

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
                "Drop Sword / Axe to activate",
                "",
                "Shoot out up to " + getValueString(this::getFinalNumLines, level) + " tracking lines that will fly",
                "towards the nearest enemies within " + getValueString(this::getMaxDistance, level) + " blocks",
                "",
                "Players hit with these blood lines will receive",
                "<effect>Glowing</effect> for " + getValueString(this::getEffectDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public int getFinalNumLines(int level) {
        return (numLines + (level - 1));
    }

    public int getEffectDuration(int level){
        return effectDuration + level * effectDurationIncreasePerLevel;
    }

    public double getMaxDistance(int level) {
        return maxDistance;
    }

    private void findEnemies(Player player, int level) {
        List<Player> enemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), (maxDistance + maxDistanceIncreasePerLevel));
        enemies.sort(Comparator.comparingDouble(p -> p.getLocation().distance(player.getLocation())));
        enemies = enemies.subList(0, Math.min(enemies.size(), getFinalNumLines(level)));

        List<List<Location>> markers = new ArrayList<>();
        WeakHashMap<Integer, Player> lineToPlayerMap = new WeakHashMap<>();

        if (!enemies.isEmpty()) {
            int lineIndex = 0;
            for (Player enemy : enemies) {
                List<Location> points = calculatePoints(player, enemy);
                if (!points.isEmpty()) {
                    markers.add(points);
                    lineToPlayerMap.put(lineIndex++, enemy);
                }
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
        } else {
            UtilMessage.message(player, getClassType().getName(), "Blood Compass failed.");
        }

        playerMarkersMap.put(player, markers);
        playerLineToPlayerMap.put(player, lineToPlayerMap);
    }

    @Override
    public void toggle(Player player, int level) {
        resetDrawingState(player);
        findEnemies(player, level);

        startDrawingLines(player);
    }

    private void resetDrawingState(Player player) {
        Integer taskId = playerTaskIdMap.get(player);
        if (taskId != null && taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        playerMarkersMap.put(player, new ArrayList<>());
        playerLineToPlayerMap.put(player, new WeakHashMap<>());
        playerTaskIdMap.put(player, -1);
    }

    private void startDrawingLines(Player player) {
        Integer existingTaskId = playerTaskIdMap.get(player);
        if (existingTaskId != null && existingTaskId != -1) {
            Bukkit.getScheduler().cancelTask(existingTaskId);
        }

        List<List<Location>> markers = playerMarkersMap.get(player);
        Map<Integer, Integer> currentPoints = new HashMap<>();
        for (int i = 0; i < markers.size(); i++) {
            currentPoints.put(i, 0);
        }

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!drawLineSegment(player, currentPoints)) {
                    this.cancel();
                }
            }
        }.runTaskTimer(champions, 0L, 1L).getTaskId();

        playerTaskIdMap.put(player, taskId);
    }

    /**
     * Draws a single line segment for each line and progresses to the next one.
     *
     * @return true if there are more segments to draw, false otherwise.
     */

    private boolean drawLineSegment(Player player, Map<Integer, Integer> currentPoints) {
        boolean moreSegmentsToDraw = false;
        List<List<Location>> markers = playerMarkersMap.get(player);
        Map<Integer, Player> lineToPlayerMap = playerLineToPlayerMap.get(player);

        for (Map.Entry<Integer, Integer> entry : currentPoints.entrySet()) {
            int lineIndex = entry.getKey();
            int currentPointIndex = entry.getValue();
            List<Location> points = markers.get(lineIndex);
            Player target = lineToPlayerMap.get(lineIndex);

            if (target != null && target.isOnline()) {
                Location originalTargetLocation = points.get(points.size() - 1);

                if (currentPointIndex == points.size() - 2 && target.getLocation().distance(originalTargetLocation) <= (escapeRadius + escapeRadiusIncreasePerLevel)) {
                    points.set(points.size() - 1, target.getLocation());
                }

                if (currentPointIndex < points.size() - 1) {
                    createLine(player, points.get(currentPointIndex), points.get(currentPointIndex + 1));
                    currentPoints.put(lineIndex, currentPointIndex + 1);
                    moreSegmentsToDraw = true;
                }
            }
        }

        return moreSegmentsToDraw;
    }

    private List<Location> calculatePoints(Player player, Player enemy) {
        List<Location> points = new ArrayList<>();
        Location start = player.getLocation().add(0, 1.8, 0);
        Location end = enemy.getLocation().add(0, 1.8, 0);
        Vector difference = end.toVector().subtract(start.toVector());

        points.add(start);

        boolean obstruction = false;
        for (int i = 1; i <= numPoints; i++) {
            double fraction = (double) i / (numPoints + 1);
            Location point = start.clone().add(difference.clone().multiply(fraction));

            double heightFactor = Math.sin(Math.PI * fraction) * 10;
            double waveFactor = Math.sin(2 * Math.PI * fraction) * 2;
            point.add(0, heightFactor, 0);
            point.add(waveFactor, waveFactor, waveFactor);

            Random random = UtilMath.RANDOM;
            point.add(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);

            points.add(point);
        }

        if (!obstruction) {
            points.add(end);
        }
        return points;
    }


    public void createLine(Player player, Location start, Location end) {
        World world = start.getWorld();
        double distance = start.distance(end);
        if (distance <= 0) {
            return;
        }

        int points = (int) (distance * 10);
        Random random = UtilMath.RANDOM;
        int level = getLevel(player);

        for (int i = 0; i <= points; i++) {
            double fraction = (double) i / points;
            Location point = start.clone().add(end.clone().subtract(start).multiply(fraction));

            float size = 0.5f + random.nextFloat();
            if (size > 1.5f) {
                size = 1.5f;
            }

            world.spawnParticle(Particle.REDSTONE, point, 1, new Particle.DustOptions(Color.RED, size));

            if (i == points) {
                Player target = world.getPlayers().stream().filter(p -> p.getLocation().equals(end)).findFirst().orElse(null);
                if (target != null) {
                    final List<Player> nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), maxDistance + maxDistanceIncreasePerLevel);
                    show(player, nearbyAllies, target);

                    player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                    UtilMessage.message(target, getClassType().getName(), "<alt2>" + player.getName() + "</alt2> hit you with <alt>" + getName() + "</alt>.");
                    UtilMessage.message(player, getClassType().getName(), "You hit <alt2>" + target.getName() + "</alt2> with <alt>" + getName() + "</alt>.");

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            hide(player, nearbyAllies, target);
                        }
                    }.runTaskLater(champions, 20L * getEffectDuration(level));
                }
            }
        }
    }

    private void show(Player player, List<Player> allies, Player target) {
        UtilPlayer.setGlowing(player, target, true);
        for (Player ally : allies) {
            UtilPlayer.setGlowing(ally, target, true);
        }
    }

    private void hide(Player player, List<Player> allies, Player target) {
        UtilPlayer.setGlowing(player, target, false);
        for (Player ally : allies) {
            UtilPlayer.setGlowing(ally, target, false);
        }
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
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
        numLines = getConfig("numLines", 1, Integer.class);
        maxDistance = getConfig("maxDistance", 64.0, Double.class);
        effectDuration = getConfig("effectDuration", 6, Integer.class);
        numPoints = getConfig("numPoints", 30, Integer.class);
        escapeRadius = getConfig("escapeRadius", 8.0, Double.class);
        effectDurationIncreasePerLevel = getConfig("effectDurationIncreasePerLevel", 1, Integer.class);
        maxDistanceIncreasePerLevel = getConfig("maxDistanceIncreasePerLevel", 0.0, Double.class);
        escapeRadiusIncreasePerLevel = getConfig("escapeRadiusIncreasePerLevel", 0.0, Double.class);
    }
}
