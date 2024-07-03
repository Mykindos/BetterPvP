package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Set;

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

    public List<Location> getPoints() {
        return points;
    }

    public void setPoints(List<Location> points) {
        this.points = points;
    }

    public int getPointIndex() {
        return pointIndex;
    }

    public void setPointIndex(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public Set<LivingEntity> getAffectedEntities() {
        return affectedEntities;
    }

    public void setAffectedEntities(Set<LivingEntity> affectedEntities) {
        this.affectedEntities = affectedEntities;
    }

    public Set<LivingEntity> getDamagedEntities() {
        return damagedEntities;
    }

    public void setDamagedEntities(Set<LivingEntity> damagedEntities) {
        this.damagedEntities = damagedEntities;
    }
}
