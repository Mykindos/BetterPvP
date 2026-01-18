package me.mykindos.betterpvp.core.client.achievements.impl.general.deaths;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.CombatCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
/**
 * The category, which defines the types and display view
 */
@SubCategory(CombatCategory.class)
public class DeathCategory extends AchievementCategory {
    public DeathCategory() {
        super(AchievementCategories.DEATH_TYPE);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.BOOK)
                .displayName(UtilMessage.deserialize("<white>Deaths"))
                .build();
    }
}
