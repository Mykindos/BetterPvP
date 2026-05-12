package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.skeletonking;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.EventCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
@SubCategory(EventCategory.class)
public class SkeletonKingKillCategory extends AchievementCategory {

    public SkeletonKingKillCategory() {
        super(AchievementCategories.EVENT_BOSS_SKELETON_KING);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.WITHER_SKELETON_SKULL)
                .displayName(UtilMessage.deserialize("<white>Kill the Skeleton King"))
                .build();
    }
}

