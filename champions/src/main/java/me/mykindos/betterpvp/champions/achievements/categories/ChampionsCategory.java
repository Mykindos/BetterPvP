package me.mykindos.betterpvp.champions.achievements.categories;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;

@Singleton
public class ChampionsCategory extends AchievementCategory {
    @Inject
    public ChampionsCategory() {
        super(AchievementCategories.CHAMPIONS);
    }

    /**
     * Get the set of Classes of containers that use this category
     *
     * @return
     */

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Translations.component("core.achievement.category.champions.name").color(NamedTextColor.WHITE))
                .lore(
                        List.of(Translations.component("core.achievement.category.champions.description"))
                )
                .material(Material.BOW)
                .build();
    }
}
