package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@BPvPListener
public class Siphon extends Skill implements PassiveSkill, MovementSkill, BuffSkill, HealthSkill, OffensiveSkill, Listener {

    /**
     * Everytime this skill updates, if the enemy is still close enough to the player, +1 is added to the successful
     * ticks for that enemy. Whenever an unsuccessful update is encountered, the count resets. Once the count is equal
     * to two elapsed seconds (that's the current number at least), the ability will proc and the count reset.
     */
    private final Map<UUID, Map<UUID, Integer>> siphonData = new ConcurrentHashMap<>();
    private final long SIPHON_UPDATE_DELAY = 250;  // Siphon updates every 250 ticks

    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double baseEnergySiphoned;
    private double energySiphonedIncreasePerLevel;
    private int speedStrength;
    private double speedDuration;
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
    public String[] getDescription(int level) {
        String duration = getValueString(this::getSpeedDuration, level);


        return new String[]{
                "Drain " + getValueString(this::getEnergySiphoned, level) + " energy per second from all enemies within " + getValueString(this::getRadius, level) + " blocks,",
                "granting you <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for " + duration + " seconds.",
                "",
                "When this skill activates, you have a " + getValueString(this::getRandomSiphonHealthGainChanceAsPercentage, level) + "% chance to gain " + getValueString(this::getHealthGainedOnRandomSiphon, level) + " health",
                "",
                "This skill only activates when enemies stay within range for " + getValueString(this::getElapsedTimeToProcAbility, level) + " seconds"
        };
    }

    public double getRadius(int level) {
        return baseRadius + ((level - 1) * radiusIncreasePerLevel);
    }

    public double getEnergySiphoned(int level) {
        return baseEnergySiphoned + ((level - 1) * energySiphonedIncreasePerLevel);
    }

    public double getSpeedDuration(int level) {
        return speedDuration;
    }

    public double getElapsedTimeToProcAbility(int level) {
        return elapsedTimeToProcAbility;
    }

    private long getRequiredSuccessfulUpdates(int level) {

        // 1000ms in a second
        double updatesPerSecond = ((double) 1000) / SIPHON_UPDATE_DELAY;

        // Elapsed time is given in seconds
        return Math.round( updatesPerSecond*getElapsedTimeToProcAbility(level) );
    }

    private double getRandomSiphonHealthGainChance(int level) {
        return randomSiphonHealthGainChance;
    }
    private int getRandomSiphonHealthGainChanceAsPercentage(int level) {
        return (int) Math.round(getRandomSiphonHealthGainChance(level)*100);
    }

    private double getHealthGainedOnRandomSiphon(int level) {
        return healthGainedOnRandomSiphon;
    }


    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }


    @EventHandler
    public void onEquip(SkillEquipEvent event) {
        if (event.getSkill().equals(this)) {
            siphonData.put(event.getPlayer().getUniqueId(), new HashMap<>());
        }
    }

    @EventHandler
    public void onDequip(SkillDequipEvent event) {
        if (event.getSkill().equals(this)) {
            siphonData.remove(event.getPlayer().getUniqueId());
        }
    }

    @UpdateEvent (delay = 500)
    public void removeActives() {
        siphonData.keySet().forEach(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || getLevel(player) <= 0) siphonData.remove(playerUUID);
        });
    }

    @UpdateEvent(delay = SIPHON_UPDATE_DELAY)
    public void monitorActives() {
        siphonData.keySet().forEach(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) return;

            Map<UUID, Integer> enemyDataMap = siphonData.get(playerUUID);
            int level = getLevel(player);
            List<LivingEntity> nearbyEnemies = UtilEntity.getNearbyEnemies(player, player.getLocation(), getRadius(level));

            // First, remove the enemies that ran away or died
            for (UUID enemyUUID : enemyDataMap.keySet()) {
                Entity entity = Bukkit.getEntity(enemyUUID);

                if (!(entity instanceof LivingEntity enemyAsLivingEnt)) {
                    enemyDataMap.remove(enemyUUID);
                    continue;
                }

                if (enemyAsLivingEnt.isDead() || !(nearbyEnemies.contains(enemyAsLivingEnt))) {
                    enemyDataMap.remove(enemyUUID);
                }
            }

            long requiredUpdates = getRequiredSuccessfulUpdates(level);

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
        });
    }

    public void activateSiphon(Player player, LivingEntity target) {
        int level = getLevel(player);

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
                    if (Math.random() < getRandomSiphonHealthGainChance(level)) {
                        double healthToGain = getHealthGainedOnRandomSiphon(level);
                        UtilPlayer.health(player, healthToGain);
                        UtilMessage.message(player, getName(), "You gained <alt2>%s</alt2> health.", UtilFormat.formatNumber(healthToGain));
                    }

                    long speedDuration = (long) (getSpeedDuration(level) * 1000);
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
        baseRadius = getConfig("baseRadius", 3.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);
        baseEnergySiphoned = getConfig("baseEnergySiphoned", 1.0, Double.class);
        energySiphonedIncreasePerLevel = getConfig("energySiphonedIncreasePerLevel", 0.0, Double.class);

        speedStrength = getConfig("speedStrength", 2, Integer.class);
        speedDuration = getConfig("speedDuration", 2.5, Double.class);
        elapsedTimeToProcAbility = getConfig("elapsedTimeToProcAbility", 2.0, Double.class);

        healthGainedOnRandomSiphon = getConfig("healthGainedOnRandomSiphon", 1.0D, Double.class);
        randomSiphonHealthGainChance = getConfig("randomSiphonHealthGainChance", 0.1, Double.class);
    }
}
