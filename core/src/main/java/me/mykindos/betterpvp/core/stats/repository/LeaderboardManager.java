package me.mykindos.betterpvp.core.stats.repository;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.stats.Leaderboard;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public final class LeaderboardManager extends Manager<Leaderboard<?, ?>> {

    @Override
    public Optional<Leaderboard<?, ?>> getObject(String identifier) {
        return objects.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(identifier))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    public Optional<Leaderboard<?, ?>> getViewableByName(String name) {
        return objects.entrySet().stream()
                .filter(e -> e.getValue().isViewable() && e.getKey().equalsIgnoreCase(name))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    public Map<String, ? extends Leaderboard<?, ?>> getViewable() {
        return getObjects().entrySet().stream()
                .filter(e -> e.getValue().isViewable())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
