package me.mykindos.betterpvp.core.scene.mob.target;

import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-mob aggro accumulator. Damagers build up threat; threat decays over time so a mob
 * eventually disengages once a target stops attacking. Replaces the MythicMobs threat table.
 * <p>
 * Stored by entity UUID so a dead/offline entity simply resolves to nothing when looked up.
 */
public class ThreatTable {

    private final Map<UUID, Double> threat = new HashMap<>();

    /** Adds {@code amount} threat for {@code source} (e.g. on taking damage from it). */
    public void add(LivingEntity source, double amount) {
        threat.merge(source.getUniqueId(), amount, Double::sum);
    }

    /** Subtracts {@code amount} from every entry and drops entries that fall to zero or below. */
    public void decay(double amount) {
        threat.replaceAll((uuid, value) -> value - amount);
        threat.values().removeIf(value -> value <= 0);
    }

    /** @return the UUID of the highest-threat entity, or empty if the table is empty. */
    public Optional<UUID> highest() {
        return threat.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    public double get(LivingEntity entity) {
        return threat.getOrDefault(entity.getUniqueId(), 0.0);
    }

    public void remove(LivingEntity entity) {
        threat.remove(entity.getUniqueId());
    }

    public void clear() {
        threat.clear();
    }

    public boolean isEmpty() {
        return threat.isEmpty();
    }

}
