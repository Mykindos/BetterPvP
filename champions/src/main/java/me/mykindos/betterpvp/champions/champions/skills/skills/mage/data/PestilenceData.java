package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PestilenceData {
    private final Set<LivingEntity> infectedTargets = ConcurrentHashMap.newKeySet();
    private final Map<LivingEntity, BukkitRunnable> trackingTasks = new ConcurrentHashMap<>();
    private final Map<LivingEntity, Long> infectionTimers = new ConcurrentHashMap<>();
    private final Map<LivingEntity, Player> originalCasters = new ConcurrentHashMap<>();
    private final Map<LivingEntity, Boolean> sentTrackingTrail = new ConcurrentHashMap<>();

    public Set<LivingEntity> getInfectedTargets() {
        return infectedTargets;
    }

    public Map<LivingEntity, BukkitRunnable> getTrackingTasks() {
        return trackingTasks;
    }

    public Map<LivingEntity, Long> getInfectionTimers() {
        return infectionTimers;
    }

    public Map<LivingEntity, Player> getOriginalCasters() {
        return originalCasters;
    }

    public Map<LivingEntity, Boolean> getSentTrackingTrail() {
        return sentTrackingTrail;
    }

    public void addInfected(LivingEntity entity) {
        infectedTargets.add(entity);
    }

    public void removeInfected(LivingEntity entity) {
        infectedTargets.remove(entity);
    }
}