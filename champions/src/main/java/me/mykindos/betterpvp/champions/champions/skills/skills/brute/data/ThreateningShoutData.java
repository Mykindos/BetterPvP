package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Set;

@Getter
public class ThreateningShoutData {
    private List<Location> points;
    private int pointIndex;
    private Set<LivingEntity> affectedEntities;
    private Set<LivingEntity> damagedEntities;

    public ThreateningShoutData(List<Location> points, int pointIndex, Set<LivingEntity> affectedEntities, Set<LivingEntity> damagedEntities) {
        this.points = points;
        this.pointIndex = pointIndex;
        this.affectedEntities = affectedEntities;
        this.damagedEntities = damagedEntities;
    }

    public void setPoints(List<Location> points) {
        this.points = points;
    }

    public void setPointIndex(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public void setAffectedEntities(Set<LivingEntity> affectedEntities) {
        this.affectedEntities = affectedEntities;
    }

    public void setDamagedEntities(Set<LivingEntity> damagedEntities) {
        this.damagedEntities = damagedEntities;
    }
}
