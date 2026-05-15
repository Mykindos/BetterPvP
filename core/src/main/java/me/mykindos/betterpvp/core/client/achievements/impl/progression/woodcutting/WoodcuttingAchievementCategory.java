package me.mykindos.betterpvp.core.client.achievements.impl.progression.woodcutting;

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
public class WoodcuttingAchievementCategory extends AchievementCategory {

    public WoodcuttingAchievementCategory() {
        super(AchievementCategories.PROGRESSION_WOODCUTTING);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.IRON_AXE)
                .displayName(UtilMessage.deserialize("<white>Woodcutting"))
                .build();
    }
}

