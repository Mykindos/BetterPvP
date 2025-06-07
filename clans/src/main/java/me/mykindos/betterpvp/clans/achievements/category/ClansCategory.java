package me.mykindos.betterpvp.clans.achievements.category;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

@Singleton
public class ClansCategory extends AchievementCategory {
    @Inject
    public ClansCategory() {
        super(ClansAchievementCategories.CLANS);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Component.text("Clan"))
                .lore(
                        List.of(Component.text("Clan related achievements"))
                )
                .material(Material.DIAMOND_SWORD)
                .build();
    }
}
