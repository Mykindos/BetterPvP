package me.mykindos.betterpvp.clans.champions.skills.skills.assassin.data;

import lombok.Data;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class RecallData {
    public List<TempData> locations = new ArrayList<>();
    private long time;

    public RecallData() {
        this.time = System.currentTimeMillis();
    }


    public double getHealth() {
        return locations.get(0).getHealth();
    }

    public void addLocation(Location location, double health, double max) {
        TempData loc = new TempData(location, health);
        locations.add(loc);
        if (locations.size() > max) {
            locations.remove(0);
        }
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

    }
}
