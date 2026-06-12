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
public class CoreBreakerCategory extends AchievementCategory {
    public CoreBreakerCategory() {
        super(AchievementCategories.CLANS_CORE_DAMAGE_TYPE);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.BEACON)
                .displayName(Translations.component("core.achievement.category.core-breaker.name").color(NamedTextColor.WHITE))
                .build();
    }
}

