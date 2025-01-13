package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@BPvPListener
public class Siphon extends Skill implements PassiveSkill, MovementSkill, BuffSkill, HealthSkill, OffensiveSkill, Listener {

    /**
     * Everytime this skill updates, if the enemy is still close enough to the player, +1 is added to the successful
     * ticks for that enemy. Whenever an unsuccessful update is encountered, the count resets. Once the count is equal
     * to two elapsed seconds (that's the current number at least), the ability will proc and the count reset.
     */
    private final Map<UUID, Map<UUID, Integer>> siphonData = new ConcurrentHashMap<>();
    private final long SIPHON_UPDATE_DELAY = 250;  // Siphon updates every 250ms or every 5 ticks

    @Getter
    private double radius;
    @Getter
    private double energySiphoned;
    private int speedStrength;
    @Getter
    private double speedDuration;
    @Getter
    private double elapsedTimeToProcAbility;
    private double healthGainedOnRandomSiphon;
    private double randomSiphonHealthGainChance;

    @Inject
    public Siphon(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Siphon";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Drain <val>" + getEnergySiphoned() + "</val> energy per second from all enemies within <val>" + getRadius() + "</val> blocks,",
                "granting you <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for <val>" + getSpeedDuration() + "</val> seconds.",
                "",
                "When this skill activates, you have a <val>" + getRandomSiphonHealthGainChanceAsPercentage() + "% chance to gain <val>" + getHealthGainedOnRandomSiphon() + "</val> health",
                "",
                "This skill only activates when enemies stay within range for <val>" + getElapsedTimeToProcAbility() + "</val> seconds"
        };
    }

    private long getRequiredSuccessfulUpdates() {

        // 1000ms in a second
        double updatesPerSecond = ((double) 1000) / SIPHON_UPDATE_DELAY;

        // Elapsed time is given in seconds
        return Math.round(updatesPerSecond * getElapsedTimeToProcAbility());
    }

    private double getRandomSiphonHealthGainChance() {
        return randomSiphonHealthGainChance;
    }

    private int getRandomSiphonHealthGainChanceAsPercentage() {
        return (int) Math.round(getRandomSiphonHealthGainChance() * 100);
    }

    private double getHealthGainedOnRandomSiphon() {
        return healthGainedOnRandomSiphon;
    }


    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }


    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        siphonData.put(player.getUniqueId(), new HashMap<>());
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        siphonData.remove(player.getUniqueId());
    }

    @UpdateEvent(delay = SIPHON_UPDATE_DELAY)
    public void monitorActives() {
        siphonData.keySet().removeIf(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) return true;
            if (!hasSkill(player)) return true;

            Map<UUID, Integer> enemyDataMap = siphonData.get(playerUUID);
            List<LivingEntity> nearbyEnemies = UtilEntity.getNearbyEnemies(player, player.getLocation(), getRadius());

            // First, remove the enemies that ran away or died
            Iterator<UUID> enemyDataIterator = enemyDataMap.keySet().iterator();
            while (enemyDataIterator.hasNext()) {
                UUID enemyUUID = enemyDataIterator.next();
                Entity entity = Bukkit.getEntity(enemyUUID);

                if (!(entity instanceof LivingEntity enemyAsLivingEnt)) {
                    enemyDataIterator.remove();
                    continue;
                }

                if (enemyAsLivingEnt.isDead() || !(nearbyEnemies.contains(enemyAsLivingEnt))) {
                    enemyDataIterator.remove();
                }
            }

            long requiredUpdates = getRequiredSuccessfulUpdates();

            // Handle successful siphons
            for (LivingEntity target : nearbyEnemies) {
                UUID enemyUUID = target.getUniqueId();

                int successfulUpdates = enemyDataMap.getOrDefault(enemyUUID, 0);
                successfulUpdates += 1;

                if (successfulUpdates == requiredUpdates) {
                    activateSiphon(player, target);
                    successfulUpdates = 0;
                }

                enemyDataMap.put(enemyUUID, successfulUpdates);
            }

            return false;
        });
    }

    public void activateSiphon(Player player, LivingEntity target) {
        if (target instanceof Player playerTarget) {
            championsManager.getEnergy().degenerateEnergy(playerTarget, ((float) getEnergySiphoned()) / 10.0f);
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
                    if (Math.random() < getRandomSiphonHealthGainChance()) {
                        double healthToGain = getHealthGainedOnRandomSiphon();
                        UtilPlayer.health(player, healthToGain);
                        UtilMessage.message(player, getName(), "You gained <alt2>%s</alt2> health.", UtilFormat.formatNumber(healthToGain));
                    }

                    long speedDuration = (long) (getSpeedDuration() * 1000);
                    championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength,
                            speedDuration, true);

                    this.cancel();
                    return;
                }

                Particle.END_ROD.builder().location(position).receivers(30).extra(0).spawn();
                v.multiply(0.9);
                position.add(v);
            }
        }.runTaskTimer(champions, 0L, 2);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 3.0, Double.class);
        energySiphoned = getConfig("energySiphoned", 1.0, Double.class);

        speedStrength = getConfig("speedStrength", 2, Integer.class);
        speedDuration = getConfig("speedDuration", 2.5, Double.class);
        elapsedTimeToProcAbility = getConfig("elapsedTimeToProcAbility", 2.0, Double.class);

        healthGainedOnRandomSiphon = getConfig("healthGainedOnRandomSiphon", 1.0D, Double.class);
        randomSiphonHealthGainChance = getConfig("randomSiphonHealthGainChance", 0.1, Double.class);
    }
}
