package me.mykindos.betterpvp.core.client.achievements.repository;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@CustomLog
@Getter
public class AchievementCompletionsConcurrentHashMap implements Iterable<AchievementCompletion> {
    private final ConcurrentHashMap<NamespacedKey, AchievementCompletion> allMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Season, ConcurrentHashMap<NamespacedKey, AchievementCompletion>> seasonMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Realm, ConcurrentHashMap<NamespacedKey, AchievementCompletion>> realmMap = new ConcurrentHashMap<>();

    public AchievementCompletionsConcurrentHashMap addCompletion(final AchievementCompletion achievementCompletion) {
        switch (achievementCompletion.getAchievementFilterType()) {
            case ALL ->
                allMap.put(achievementCompletion.getKey(), achievementCompletion);

            case SEASON ->
                    seasonMap.computeIfAbsent((Season) achievementCompletion.getPeriod(),
                            (k) -> new ConcurrentHashMap<>()
            ).put(achievementCompletion.getKey(), achievementCompletion);

            case REALM ->
                    realmMap.computeIfAbsent((Realm) achievementCompletion.getPeriod(),
                            (k) -> new ConcurrentHashMap<>()
                    ).put(achievementCompletion.getKey(), achievementCompletion);

        }
        return this;
    }

    public Optional<AchievementCompletion> getCompletion(@NotNull IAchievement achievement, @Nullable Period period) {
        return getCompletion(achievement.getNamespacedKey(), achievement.getAchievementFilterType(), period);
    }

    public Optional<AchievementCompletion> getCompletion(NamespacedKey namespacedKey, StatFilterType filterType, Period period) {
        switch (filterType) {
            case ALL -> {
                return Optional.ofNullable(allMap.get(namespacedKey));
            }
            case SEASON -> {
                final ConcurrentHashMap<NamespacedKey, AchievementCompletion> periodMap = seasonMap.get((Season) period);
                //there are no achievements completed during this period
                if (periodMap == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(periodMap.get(namespacedKey));
            }

            case REALM -> {
                final ConcurrentHashMap<NamespacedKey, AchievementCompletion> periodMap = realmMap.get((Realm) period);
                //there are no achievements completed during this period
                if (periodMap == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(periodMap.get(namespacedKey));
            }
        }
        log.warn("Unknown AchievementCompletion type {}", filterType).submit();
        return Optional.empty();
    }

    public void fromOther(AchievementCompletionsConcurrentHashMap other) {
        allMap.clear();
        allMap.putAll(other.allMap);
        seasonMap.clear();
        seasonMap.putAll(other.seasonMap);
        realmMap.clear();
        realmMap.putAll(other.realmMap);
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public @NotNull Iterator<AchievementCompletion> iterator() {
        Set<AchievementCompletion> completions = new HashSet<>(allMap.values());
        completions.addAll(seasonMap.values()
                .stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toSet())
        );
        completions.addAll(realmMap.values()
                .stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toSet())
        );
        return completions.iterator();
    }
}
