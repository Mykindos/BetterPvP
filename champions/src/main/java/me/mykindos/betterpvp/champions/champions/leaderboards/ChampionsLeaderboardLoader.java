package me.mykindos.betterpvp.champions.champions.leaderboards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.loader.LeaderboardLoader;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;

@Singleton
@CustomLog
public class ChampionsLeaderboardLoader extends LeaderboardLoader {

    @Inject
    public ChampionsLeaderboardLoader(LeaderboardManager leaderboardManager, Champions plugin) {
        super(leaderboardManager, plugin);
    }

    @SuppressWarnings("rawtypes")
    public void registerLeaderboards(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends Leaderboard>> classes = reflections.getSubTypesOf(Leaderboard.class);
        for (var clazz : classes) {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                load(clazz);
            }
        }

        log.info("Loaded " + count + " leaderboards for " + packageName).submit();
    }
}
