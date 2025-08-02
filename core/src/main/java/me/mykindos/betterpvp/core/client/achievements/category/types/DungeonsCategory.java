package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

@Singleton
public class DungeonsCategory extends AchievementCategory {
    @Inject
    public DungeonsCategory() {
        super(AchievementCategories.DUNGEONS);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Component.text("Dungeons"))
                .lore(
                        List.of(Component.text("Dungeon related achievements"))
                )
                .material(Material.MOSSY_COBBLESTONE)
                .build();
    }
}
