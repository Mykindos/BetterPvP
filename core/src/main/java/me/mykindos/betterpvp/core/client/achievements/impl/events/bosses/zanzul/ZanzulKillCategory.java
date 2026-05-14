package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.zanzul;

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
public class ZanzulKillCategory extends AchievementCategory {

    public ZanzulKillCategory() {
        super(AchievementCategories.EVENT_BOSS_ZANZUL);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.BLAZE_ROD)
                .displayName(UtilMessage.deserialize("<white>Kill Zanzul"))
                .build();
    }
}

