package me.mykindos.betterpvp.core.stats.repository;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.event.LeaderboardInitializeEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Optional;

@Singleton
@BPvPListener
public final class LeaderboardRepository extends Manager<Leaderboard<?, ?>> implements Listener {

    @EventHandler
    public void onLeaderboardInitialize(LeaderboardInitializeEvent event) {
        addObject(event.getLeaderboard().getName(), event.getLeaderboard());
    }

    @Override
    public Optional<Leaderboard<?, ?>> getObject(String identifier) {
        return objects.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(identifier))
                .findFirst()
                .map(Map.Entry::getValue);
    }
}
