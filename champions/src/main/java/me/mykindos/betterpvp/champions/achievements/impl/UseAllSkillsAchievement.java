package me.mykindos.betterpvp.champions.achievements.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.achievements.ChampionsAchievementCategories;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.types.NSingleGoalSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Singleton
@BPvPListener
public class UseAllSkillsAchievement extends NSingleGoalSimpleAchievement {

    @Inject
    public UseAllSkillsAchievement(ChampionsSkillManager skillManager) {
        super("Use All Skills", new NamespacedKey("champions", "use_all_skills"), ChampionsAchievementCategories.CHAMPIONS, AchievementType.GLOBAL, 60_000d, getAllSkills(skillManager));
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

    @Override
    public String getName() {
        return "Use All Skills";
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

    private List<Component> getRemainingElements(StatContainer statContainer) {
        List<ChampionsSkillStat> neededStats = getWatchedStats().stream()
                .map(ChampionsSkillStat.class::cast)
                .filter(stat -> calculateCurrentElementPercent(statContainer, stat) < 1.0f)
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
