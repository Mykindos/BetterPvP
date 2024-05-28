package me.mykindos.betterpvp.core.stats.loader;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;

@CustomLog
@Singleton
public class LeaderboardLoader extends Loader {

    private final LeaderboardManager leaderboardManager;
    private final Adapters adapters;

    public LeaderboardLoader(LeaderboardManager leaderboardManager, BPvPPlugin plugin) {
        super(plugin);
        this.leaderboardManager = leaderboardManager;
        this.adapters = new Adapters(plugin);
    }

    public void register(Leaderboard<?, ?> leaderboard) {
        leaderboardManager.addObject(leaderboard.getName(), leaderboard);
        count++;
    }


    @Override
    public void load(Class<?> clazz) {
        if (!adapters.canLoad(clazz)) {
            log.warn("Could not load leaderboard " + clazz.getSimpleName() + "! Dependencies not found!").submit();
            return;
        }

        try {
            Leaderboard<?, ?> leaderboard = (Leaderboard<?, ?>) plugin.getInjector().getInstance(clazz);
            register(leaderboard);
        } catch (Exception ex) {
            log.error("Failed to load leaderboard", ex).submit();
        }
    }


}
