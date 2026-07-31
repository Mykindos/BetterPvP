package me.mykindos.betterpvp.clans.world;

import com.google.inject.Singleton;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Registry of predicates deciding whether a non-main world should behave like the main world for survival-mode
 * purposes. {@link me.mykindos.betterpvp.clans.clans.listeners.ClansWorldListener} forces every non-main world into
 * adventure mode by default; systems that own their own survival-capable worlds (e.g. discovery islands) register a
 * matcher here instead of that listener growing feature-specific knowledge.
 */
@Singleton
public class SurvivalWorlds {

    private final List<Predicate<World>> matchers = new CopyOnWriteArrayList<>();

    public void register(@NotNull Predicate<World> matcher) {
        matchers.add(matcher);
    }

    public boolean allows(@NotNull World world) {
        return matchers.stream().anyMatch(matcher -> matcher.test(world));
    }

}
