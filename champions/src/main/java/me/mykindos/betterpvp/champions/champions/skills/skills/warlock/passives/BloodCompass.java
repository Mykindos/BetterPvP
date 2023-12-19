package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class BloodCompass extends Skill implements ToggleSkill, CooldownSkill {

    private int numLines;
    public double maxDistance;
    public int effectDuration;
    public int numPoints;
    public double escapeRadius;
    public double damage;
    public double damageIncreasePerLevel;
    public double escapeRadiusIncreasePerLevel;
    public double maxDistanceIncreasePerLevel;
    private final List<List<Location>> markers = new ArrayList<>();
    private final Map<Integer, Player> lineToPlayerMap = new HashMap<>();
    private Map<Integer, Integer> currentPoints;
    private int taskId = -1;



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
                "Shoot out up to <val>" + getFinalNumLines(level) + "</val> tracking lines that will fly",
                "towards the nearest enemies within <stat>" + maxDistance + "</stat> blocks",
                "",
                "Players hit with these blood lines will receive",
                "<effect>Glowing</effect> for <stat>" + effectDuration + "</stat> seconds and take <val>" + getFinalDamage(level) +"</val> damage",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getFinalDamage(int level){
        return (damage + ((level - 1)) * damageIncreasePerLevel);
    }

    public int getFinalNumLines(int level){
        return (numLines + (level - 1));
    }

    private void findEnemies(Player player, int level) {
        markers.clear();
        lineToPlayerMap.clear();
        List<Player> enemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), (maxDistance + maxDistanceIncreasePerLevel));
        enemies.sort(Comparator.comparingDouble(p -> p.getLocation().distance(player.getLocation())));
        enemies = enemies.subList(0, Math.min(enemies.size(), getFinalNumLines(level)));

        if (enemies.isEmpty()) {
            UtilMessage.message(player, getClassType().getName(), "Blood Compass failed.");
        } else {
            int lineIndex = 0;
            for (Player enemy : enemies) {
                calculatePoints(player, enemy);
                lineToPlayerMap.put(lineIndex++, enemy);
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
        }
    }

    @Override
    public void toggle(Player player, int level) {
        resetDrawingState();
        findEnemies(player, level);

        for (int lineIndex = 0; lineIndex < markers.size(); lineIndex++) {
            List<Location> points = markers.get(lineIndex);
            Player target = lineToPlayerMap.get(lineIndex);
            boolean pathIsClear = true;

            if (target != null && target.isOnline()) {
                for (int i = 1; i <= points.size(); i++) {
                    Location endPoint = i < points.size() ? points.get(i) : target.getLocation().add(0, 1.8, 0);
                    boolean segmentClear = isLineClear(points.get(i - 1), endPoint);
                    if (!segmentClear) {
                        pathIsClear = false;
                        break;
                    }
                }

                if (pathIsClear) {
                    drawCircle(target.getLocation(), (escapeRadius + escapeRadiusIncreasePerLevel));
                }
            }
        }

        startDrawingLines(player);
    }

    private void resetDrawingState() {
        currentLine = 0;
        currentPoint = 0;
        markers.clear();
    }

    private int currentLine = 0;
    private int currentPoint = 0;

    private void startDrawingLines(Player player) {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        currentPoints = new HashMap<>();
        for (int i = 0; i < markers.size(); i++) {
            currentPoints.put(i, 0);
        }

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!drawLineSegment(player)) {
                    this.cancel();
                }
            }
        }.runTaskTimer(champions, 0L, 1L).getTaskId();
    }

    /**
     * Draws a single line segment for each line and progresses to the next one.
     * @return true if there are more segments to draw, false otherwise.
     */

    private boolean drawLineSegment(Player player) {
        boolean moreSegmentsToDraw = false;

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

    private boolean isLineClear(Location start, Location end) {
        World world = start.getWorld();
        if (!world.equals(end.getWorld())) {
            return false;
        }

        int points = (int) (start.distance(end) * 5);

        for (int i = 0; i <= points; i++) {
            double fraction = (double) i / points;
            Location point = start.clone().add(end.clone().subtract(start).multiply(fraction));

            if (UtilBlock.solid(point.getBlock())) {
                return false;
            }
        }
        return true;
    }

    private void calculatePoints(Player player, Player enemy) {
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

            Random random = new Random();
            point.add(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);

            if (i > 1 && !isLineClear(points.get(points.size() - 1), point)) {
                obstruction = true;
                break;
            }
            points.add(point);
        }

        if (!obstruction) {
            points.add(end);
        }

        markers.add(points);
    }


    public void createLine(Player player, Location start, Location end) {
        World world = start.getWorld();
        double distance = start.distance(end);
        int points = (int) (distance * 10);
        Random random = new Random();
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
                    UtilPlayer.setGlowing(player, target, true);
                    target.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                    target.damage(getFinalDamage(level));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            UtilPlayer.setGlowing(player, target, false);
                        }
                    }.runTaskLater(champions, 20L * effectDuration);
                }
            }
        }
    }

    private void drawCircle(Location center, double radius) {
        World world = center.getWorld();
        int particles = 50;

        for (int i = 0; i < particles; i++) {
            double angle = 2 * Math.PI * i / particles;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location loc = center.clone().add(x, 0, z);

            while (loc.getBlockY() > 0 && UtilBlock.airFoliage(loc.getBlock())) {
                loc.subtract(0, 1, 0);
            }
            loc.add(0, 1.25, 0);

            world.spawnParticle(Particle.REDSTONE, loc, 1, new Particle.DustOptions(Color.RED, 2.0f));
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
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        numLines = getConfig("numLines", 1, Integer.class);
        maxDistance = getConfig("maxDistance", 64.0, Double.class);
        effectDuration = getConfig("effectDuration", 5, Integer.class);
        numPoints = getConfig("numPoints", 30, Integer.class);
        escapeRadius = getConfig("escapeRadius", 7.0, Double.class);
        damage = getConfig("damage", 7.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 2.0, Double.class);
        maxDistanceIncreasePerLevel = getConfig("maxDistanceIncreasePerLevel",0.0, Double.class);
        escapeRadiusIncreasePerLevel = getConfig("escapeRadiusIncreasePerLevel",0.0, Double.class);
    }
}
