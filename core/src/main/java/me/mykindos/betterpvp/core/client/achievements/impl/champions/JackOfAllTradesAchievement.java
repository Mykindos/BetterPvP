package me.mykindos.betterpvp.core.client.achievements.impl.champions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.NSingleGoalSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.champions.RoleStat;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
@CustomLog
public class JackOfAllTradesAchievement extends NSingleGoalSimpleAchievement {

    private static final long GOAL_MS = 3_600_000L; // 1 hour per role

    @Inject
    public JackOfAllTradesAchievement() {
        super("Jack of All Trades",
                new NamespacedKey("champions", "jack_of_all_trades"),
                AchievementCategories.CHAMPIONS,
                StatFilterType.ALL,
                GOAL_MS,
                getAllRoleStats()
        );
    }

    private static GenericStat[] getAllRoleStats() {
        return Arrays.stream(Role.values())
                .map(role -> new GenericStat(
                        RoleStat.builder()
                                .action(RoleStat.Action.TIME_PLAYED)
                                .role(role)
                                .build()
                ))
                .toArray(GenericStat[]::new);
    }

    @Override
    public Description getDescription(StatContainer container, StatFilterType type, Period period) {
        List<Component> lore = new ArrayList<>(
                List.of(
                        Translations.component("core.achievement.jack-of-all-trades.desc",
                                Translations.component("core.achievement.jack-of-all-trades.one-hour").color(NamedTextColor.YELLOW)).color(NamedTextColor.GRAY)
                ));
        lore.addAll(getRemainingRoles(container));
        lore.addAll(this.getProgressComponent(container, type, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.ENCHANTED_BOOK)
                .displayName(Translations.component("core.achievement.jack-of-all-trades.name").color(NamedTextColor.WHITE))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }

    @Override
    public List<Component> getProgressComponent(StatContainer container, StatFilterType type, @Nullable Period period) {
        int completed = (int) getWatchedStats().stream()
                .filter(stat -> calculateCurrentElementPercent(container, stat) >= 1.0f)
                .count();
        int total = getWatchedStats().size();
        List<Component> progressComponent = new ArrayList<>(super.getProgressComponent(container, type, period));
        Component bar = progressComponent.getFirst();
        progressComponent.removeFirst();
        progressComponent.addFirst(bar.append(Translations.component("core.achievement.jack-of-all-trades.fraction",
                Component.text(completed, NamedTextColor.GREEN),
                Component.text(total, NamedTextColor.YELLOW))));
        return progressComponent;
    }

    private List<Component> getRemainingRoles(StatContainer statContainer) {
        List<GenericStat> remaining = getWatchedStats().stream()
                .filter(stat -> calculateCurrentElementPercent(statContainer, stat) < 1.0f)
                .map(GenericStat.class::cast)
                .toList();

        List<Component> components = new ArrayList<>();
        if (remaining.isEmpty()) return List.of();
        components.add(Translations.component("core.achievement.jack-of-all-trades.remaining").color(NamedTextColor.GRAY));
        final int toShow = Math.min(remaining.size(), 3);
        for (int i = 0; i < toShow; i++) {
            RoleStat roleStat = (RoleStat) remaining.get(i).getStat();
            Role role = roleStat.getRole();
            long currentMs = roleStat.getStat(statContainer, StatFilterType.ALL, null);
            Duration timeRemaining = Duration.of(Math.max(0, GOAL_MS - currentMs), ChronoUnit.MILLIS);
            Component roleName = role != null
                    ? role.getDisplayName().color(NamedTextColor.WHITE)
                    : Translations.component("core.achievement.jack-of-all-trades.unknown-role").color(NamedTextColor.WHITE);
            components.add(Translations.component("core.achievement.jack-of-all-trades.role-remaining",
                    roleName,
                    Component.text(UtilTime.humanReadableFormat(timeRemaining), NamedTextColor.GRAY)).color(NamedTextColor.GRAY));
        }
        if (remaining.size() > 3) {
            components.add(Translations.component("core.achievement.jack-of-all-trades.more",
                    Component.text(remaining.size() - toShow, NamedTextColor.GREEN)).color(NamedTextColor.WHITE));
        }
        return components;
    }
}

