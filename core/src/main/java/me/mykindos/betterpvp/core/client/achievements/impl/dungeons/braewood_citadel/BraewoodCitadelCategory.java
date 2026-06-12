package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.braewood_citadel;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.DungeonsCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Singleton
/**
 * The category, which defines the types and display view
 */
@SubCategory(DungeonsCategory.class)
public class BraewoodCitadelCategory extends AchievementCategory {

    public BraewoodCitadelCategory() {
        super(AchievementCategories.DUNGEONS_BRAEWOOD_CITADEL_PERIOD);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.CHISELED_STONE_BRICKS)
                .displayName(Translations.component("core.achievement.category.braewood-citadel.name").color(NamedTextColor.WHITE))
                .build();
    }
}

