package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@BPvPListener
public class Intimidation extends Skill implements PassiveSkill, DebuffSkill {

    private double radius;
    private int slownessStrength;
    private int weaknessStrength;
    private double radiusIncreasePerLevel;
    private double hitboxSize;

    private final AtomicInteger soundTicks = new AtomicInteger(0);
    private final WeakHashMap<Player, Set<Player>> trackedEnemies = new WeakHashMap<>();

    @Inject
    public Intimidation(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Intimidation";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Enemies you are looking at within " + getValueString(this::getRadius, level) + " blocks receive <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> and <effect>Weakness " + UtilFormat.getRomanNumeral(weaknessStrength) + "</effect>."
        };
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        trackedEnemies.put(player, new HashSet<>());
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        if (!trackedEnemies.containsKey(player)) return;
        for (Player tracked : trackedEnemies.get(player)) {
            UtilPlayer.clearWarningEffect(tracked);
        }
        trackedEnemies.remove(player);
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    @UpdateEvent
    public void onUpdate() {
        final boolean playSound = soundTicks.get() == 0;
        final Iterator<Player> iterator = trackedEnemies.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (level < 0) {
                iterator.remove();
                continue;
            }

            intimidateEnemies(player, level, playSound);
        }

        if (soundTicks.addAndGet(1) >= 20) {
            soundTicks.set(0);
        }
    }

    public void intimidateEnemies(Player player, int level, boolean playSound) {
        double radius = getRadius(level);
        List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius);

        // Get or create the tracked enemies set for the player
        Set<Player> tracked = trackedEnemies.computeIfAbsent(player, k -> new HashSet<>());

        // Remove enemies that are no longer nearby or no longer being looked at
        tracked.removeIf(enemy -> {
            boolean remove = !nearbyEnemies.contains(enemy) || !enemy.isOnline() || !isLookingAt(player, enemy, radius, 0.5);
            if (remove) {
                UtilPlayer.clearWarningEffect(enemy);
                UtilPlayer.setGlowing(player, enemy, false);

            }
            return remove;
        });

        for (Player enemy : nearbyEnemies) {
            if (isLookingAt(player, enemy, radius, hitboxSize)) {
                // Apply slowness and weakness effects
                championsManager.getEffects().addEffect(enemy, player, EffectTypes.SLOWNESS, getName(), slownessStrength, 500, true);
                championsManager.getEffects().addEffect(enemy, player, EffectTypes.WEAKNESS, getName(), weaknessStrength, 500, true);

                // Play sound effects and manage warning effects
                if (tracked.add(enemy)) {
                    UtilPlayer.setWarningEffect(enemy, 1);
                    UtilPlayer.setGlowing(player, enemy, true);
                }
                if (playSound) {
                    UtilSound.playSound(enemy, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f, true);
                }
            } else if (tracked.remove(enemy)) {
                UtilPlayer.setGlowing(player, enemy, false);
                UtilPlayer.clearWarningEffect(enemy);
            }
        }
    }

    // Utility method to check if a player is looking at another player using ray tracing
    private boolean isLookingAt(Player player, Player target, double maxDistance, double hitboxSize) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // Perform ray trace
        RayTraceResult result = player.getWorld().rayTrace(
                eyeLocation,
                direction,
                maxDistance,
                FluidCollisionMode.NEVER,
                true,
                hitboxSize,
                entity -> entity.equals(target)
        );

        return result != null && result.getHitEntity() != null && result.getHitEntity().equals(target);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 4.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
        weaknessStrength = getConfig("weaknessStrength", 1, Integer.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 3.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.5, Double.class);
    }
}