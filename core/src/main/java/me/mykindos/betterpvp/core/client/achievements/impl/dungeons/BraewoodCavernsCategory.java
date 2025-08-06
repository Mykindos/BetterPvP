package me.mykindos.betterpvp.core.client.achievements.impl.dungeons;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.DungeonsCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
/**
 * The category, which defines the types and display view
 */
@SubCategory(DungeonsCategory.class)
public class BraewoodCavernsCategory extends AchievementCategory {
    public BraewoodCavernsCategory() {
        super(AchievementCategories.DUNGEONS_BRAEWOOD_CAVERNS_PERIOD);
    }


    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.OAK_SAPLING)
                .displayName(UtilMessage.deserialize("<white>Defeat Braewood Caverns"))
                .build();
    }
}
