package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.ClansCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Singleton
@SubCategory(ClansCategory.class)
public class DemolisherCategory extends AchievementCategory {
    public DemolisherCategory() {
        super(AchievementCategories.CLANS_CANNON_BLOCK_DAMAGE_TYPE);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.TNT)
                .displayName(Translations.component("core.achievement.category.demolisher.name").color(NamedTextColor.WHITE))
                .build();
    }
}

