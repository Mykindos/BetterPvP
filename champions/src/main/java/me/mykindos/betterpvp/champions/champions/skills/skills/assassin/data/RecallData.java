package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.Data;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class RecallData {
    public List<TempData> locations = new ArrayList<>();
    private List<LocationMarker> locationMarkers = new ArrayList<>();

    private long time;

    public RecallData() {
        this.time = System.currentTimeMillis();
    }


    public double getHealth() {
        return locations.get(0).getHealth();
    }

    public void addLocationMarker(Location location) {
        locationMarkers.add(new LocationMarker(location));
    }
    public List<LocationMarker> getLocationMarkers() {
        return locationMarkers;
    }
    public void addLocation(Location location, double health, double max) {
        TempData loc = new TempData(location, health);
        locations.add(loc);
        if (locations.size() > max) {
            locations.remove(0);
        }
    }

    public double getOldHealth(double secondsAgo) {
        for (TempData data : locations) {
            if (data.getTimestamp() <= System.currentTimeMillis() - (secondsAgo * 1000)) {
                return data.getHealth();
            }
        }
        return -1;
    }


    public Location getLocation() {
        return locations.get(0).getLocation();
    }


    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Data
    private static class TempData {
        private final Location location;
        private final double health;
        private final long timestamp;

        public TempData(Location location, double health) {
            this.location = location;
            this.health = health;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class LocationMarker {
        private final Location location;
        private final long timestamp;

        public LocationMarker(Location location) {
            this.location = location;
            this.timestamp = System.currentTimeMillis();
        }

        public Location getLocation() {
            return location;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
