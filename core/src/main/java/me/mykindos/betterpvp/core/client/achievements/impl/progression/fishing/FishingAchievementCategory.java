package me.mykindos.betterpvp.core.client.achievements.impl.progression.fishing;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.ProgressionCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
@SubCategory(ProgressionCategory.class)
public class FishingAchievementCategory extends AchievementCategory {

    public FishingAchievementCategory() {
        super(AchievementCategories.PROGRESSION_FISHING);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.FISHING_ROD)
                .displayName(UtilMessage.deserialize("<white>Fishing"))
                .build();
    }
}

