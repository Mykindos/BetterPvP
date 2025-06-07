package me.mykindos.betterpvp.core.client.achievements.repository;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public class AchievementCompletionsConcurrentHashMap implements Iterable<AchievementCompletion> {
    private final ConcurrentHashMap<String, ConcurrentHashMap<NamespacedKey, AchievementCompletion>> backingMap = new ConcurrentHashMap<>();

    public AchievementCompletionsConcurrentHashMap addCompletion(final AchievementCompletion achievementCompletion) {
        backingMap.compute(achievementCompletion.getPeriod(), (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<>();
            }
            v.put(achievementCompletion.getKey(), achievementCompletion);
            return v;
        });
        return this;
    }

    public Optional<AchievementCompletion> getCompletion(NamespacedKey achievement, String period) {
        final ConcurrentHashMap<NamespacedKey, AchievementCompletion> periodMap = backingMap.get(period);
        //there are no achievements completed during this period
        if (periodMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(periodMap.get(achievement));
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public @NotNull Iterator<AchievementCompletion> iterator() {
        return backingMap.values().stream()
                .flatMap(periodMap -> periodMap.values().stream())
                .iterator();
    }
}
