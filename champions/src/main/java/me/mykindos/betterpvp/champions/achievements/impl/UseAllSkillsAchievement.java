package me.mykindos.betterpvp.champions.achievements.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.NSingleGoalSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.listener.loader.ListenerLoader;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Singleton
@CustomLog
public class UseAllSkillsAchievement extends NSingleGoalSimpleAchievement {

    @Inject
    public UseAllSkillsAchievement(ChampionsSkillManager skillManager) {
        super("Use All Skills", new NamespacedKey("champions", "use_all_skills"),
                AchievementCategories.CHAMPIONS,
                AchievementType.GLOBAL, 60_000L,
                getAllSkills(skillManager));
        //cannot register via BPvPListener as it loads before skills are loaded
        ListenerLoader.register(JavaPlugin.getPlugin(Champions.class), this);
    }

    private static ChampionsSkillStat[] getAllSkills(ChampionsSkillManager skillManager) {
        return skillManager.getObjects().values().stream()
                .filter(Skill::isEnabled)
                .map(skill ->
                        ChampionsSkillStat.builder()
                                .action(ChampionsSkillStat.Action.TIME_PLAYED)
                                .skill(skill)
                                .build()
                )
                .toArray(ChampionsSkillStat[]::new);
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @param period
     * @return
     */
    @Override
    public Description getDescription(StatContainer container, String period) {
        List<Component> lore = new ArrayList<>(
                List.of(
                        UtilMessage.deserialize("<gray>Use all skills for 1 minute")
                ));
        lore.addAll(getRemainingElements(container));
        lore.addAll(this.getProgressComponent(container, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.ENCHANTED_BOOK )
                .displayName(UtilMessage.deserialize("<white>%s", getName()))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }

    @Override
    public List<Component> getProgressComponent(StatContainer container, @Nullable String period) {
        int completed = getWatchedStats().stream()
                .map(ChampionsSkillStat.class::cast)
                .filter(stat -> calculateCurrentElementPercent(container, stat) < 1.0f)
                .toList().size();
        int total = getWatchedStats().size();
        List<Component> progressComponent = new ArrayList<>(super.getProgressComponent(container, period));
        Component bar = progressComponent.getFirst();
        progressComponent.removeFirst();
        progressComponent.addFirst(bar.append(UtilMessage.deserialize(" (<green>%s</green>/<yellow>%s</yellow>)", completed, total)));
        return progressComponent;
    }

    private List<Component> getRemainingElements(StatContainer statContainer) {
        List<ChampionsSkillStat> neededStats = getWatchedStats().stream()
                .map(ChampionsSkillStat.class::cast)
                .filter(stat -> calculateCurrentElementPercent(statContainer, stat) >= 1.0f)
                .toList();
        List<Component> components = new ArrayList<>();
        if (neededStats.isEmpty()) return List.of();
        components.add(Component.text("Needed:"));
        for (int i = 0; i < 3; i++) {
            components.add(Component.text(Objects.requireNonNull(neededStats.get(i).getSkillName()), NamedTextColor.WHITE));
        }
        if (neededStats.size() > 3) {
            components.add(UtilMessage.deserialize("<white>+<green>%s", neededStats.size() - 3));
        }
        return components;
    }
}
