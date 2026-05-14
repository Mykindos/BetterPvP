package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.dreadbeard;

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
public class DreadbeardKillCategory extends AchievementCategory {

    public DreadbeardKillCategory() {
        super(AchievementCategories.EVENT_BOSS_DREADBEARD);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.NAUTILUS_SHELL)
                .displayName(UtilMessage.deserialize("<white>Kill Dreadbeard"))
                .build();
    }
}

