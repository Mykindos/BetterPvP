package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives.Recall;
import org.bukkit.Location;

import java.util.LinkedList;

public class RecallData {

    private final LinkedList<Location> markers = new LinkedList<>();
    private final Recall recall;
    private final int level;

    public RecallData(Recall recall, int level) {
        this.recall = recall;
        this.level = level;
    }

    public void push(Location location) {
        markers.addFirst(location);
        if (markers.size() > recall.getDuration(level) / (Recall.MARKER_MILLIS / 1000d)) {
            markers.removeLast();
        }
    }

    public LinkedList<Location> getMarkers() {
        return markers;
    }

}
