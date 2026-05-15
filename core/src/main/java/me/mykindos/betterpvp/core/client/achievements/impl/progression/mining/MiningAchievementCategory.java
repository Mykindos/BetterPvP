package me.mykindos.betterpvp.core.client.achievements.impl.progression.mining;

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
public class MiningAchievementCategory extends AchievementCategory {

    public MiningAchievementCategory() {
        super(AchievementCategories.PROGRESSION_MINING);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.IRON_PICKAXE)
                .displayName(UtilMessage.deserialize("<white>Mining"))
                .build();
    }
}

