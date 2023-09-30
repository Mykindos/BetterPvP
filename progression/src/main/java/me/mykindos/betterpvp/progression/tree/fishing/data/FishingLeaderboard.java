package me.mykindos.betterpvp.progression.tree.fishing.data;

import me.mykindos.betterpvp.progression.model.Leaderboard;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;

import java.util.SortedMap;

public class FishingLeaderboard implements Leaderboard<Fishing> {

    @Override
    public SortedMap<String, ProgressionData<Fishing>> getTop(int amount) {
        return null; // todo implement
    }

    @Override
    public void insert(ProgressionData<Fishing> data) {
        // todo implement
    }
}
