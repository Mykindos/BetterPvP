package me.mykindos.betterpvp.core.client.achievements.repository;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@CustomLog
@Getter
public class AchievementCompletionsConcurrentHashMap {
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

    public Optional<AchievementCompletion> getCompletion(IAchievement achievement, Object period) {
        switch (achievement.getAchievementFilterType()) {
            case ALL -> {
                return Optional.ofNullable(allMap.get(achievement.getNamespacedKey()));
            }
            case SEASON -> {
                final ConcurrentHashMap<NamespacedKey, AchievementCompletion> periodMap = seasonMap.get((Season) period);
                //there are no achievements completed during this period
                if (periodMap == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(periodMap.get(achievement.getNamespacedKey()));
            }

            case REALM -> {
                final ConcurrentHashMap<NamespacedKey, AchievementCompletion> periodMap = realmMap.get((Realm) period);
                //there are no achievements completed during this period
                if (periodMap == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(periodMap.get(achievement.getNamespacedKey()));
            }
        }
        log.warn("Unknown AchievementCompletion type {}", achievement.getAchievementFilterType()).submit();
        return Optional.empty();
    }

    public @NotNull ConcurrentMap<NamespacedKey, AchievementCompletion> asMap() {
        final ConcurrentMap<NamespacedKey, AchievementCompletion> completions = new ConcurrentHashMap<>(allMap);

        completions.putAll(seasonMap.values().stream()
                .flatMap(periodMap -> periodMap.entrySet().stream())
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ))
        );

        completions.putAll(realmMap.values().stream()
                .flatMap(periodMap -> periodMap.entrySet().stream())
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ))
        );

        return completions;
    }

    public void fromOther(AchievementCompletionsConcurrentHashMap other) {
        allMap.clear();
        allMap.putAll(other.allMap);
        seasonMap.clear();
        seasonMap.putAll(other.seasonMap);
        realmMap.clear();
        realmMap.putAll(other.realmMap);
    }
}
