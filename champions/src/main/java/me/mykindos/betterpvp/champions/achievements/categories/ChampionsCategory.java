package me.mykindos.betterpvp.champions.achievements.categories;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import me.mykindos.betterpvp.champions.achievements.ChampionsAchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

@Singleton
public class ChampionsCategory extends AchievementCategory {
    @Inject
    public ChampionsCategory() {
        super(ChampionsAchievementCategories.CHAMPIONS);
    }

    /**
     * Get the set of Classes of containers that use this category
     *
     * @return
     */

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Component.text("Champions"))
                .lore(
                        List.of(Component.text("Champions related achievements"))
                )
                .material(Material.BOW)
                .build();
    }
}
