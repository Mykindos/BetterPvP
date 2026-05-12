package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.soulknight;

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
public class SoulKnightKillCategory extends AchievementCategory {

    public SoulKnightKillCategory() {
        super(AchievementCategories.EVENT_BOSS_SOUL_KNIGHT);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.SOUL_LANTERN)
                .displayName(UtilMessage.deserialize("<white>Kill the Soul Knight"))
                .build();
    }
}

