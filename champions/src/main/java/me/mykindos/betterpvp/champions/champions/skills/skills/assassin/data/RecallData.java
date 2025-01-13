package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives.Recall;
import org.bukkit.Location;

import java.util.LinkedList;

public class RecallData {

    @Getter
    private final LinkedList<Location> markers = new LinkedList<>();
    private final Recall recall;

    public RecallData(Recall recall) {
        this.recall = recall;
    }

    public void push(Location location) {
        markers.addFirst(location);
        if (markers.size() > recall.getDuration() / (Recall.MARKER_MILLIS / 1000d)) {
            markers.removeLast();
        }
    }

}
