package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ThreateningShoutData {
    private List<Location> points;
    private int pointIndex;
    private Set<LivingEntity> affectedEntities;
    private Set<LivingEntity> damagedEntities;
    private double damageToDeal;
    private double vulnerabilityDurationToApply;

    public ThreateningShoutData(List<Location> points, int pointIndex, Set<LivingEntity> affectedEntities,
                                Set<LivingEntity> damagedEntities, double damageToDeal, double vulnerabilityDurationToApply) {
        this.points = points;
        this.pointIndex = pointIndex;
        this.affectedEntities = affectedEntities;
        this.damagedEntities = damagedEntities;
        this.damageToDeal = damageToDeal;
        this.vulnerabilityDurationToApply = vulnerabilityDurationToApply;
    }
}
