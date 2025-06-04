package me.mykindos.betterpvp.clans.achievements.category;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

@Singleton
public class ClansCategory extends AchievementCategory {
    @Inject
    public ClansCategory() {
        super(ClansAchievementCategories.CLANS);
    }

    /**
     * Get the set of Classes of containers that use this category
     *
     * @return
     */
    @Override
    public Set<Class<? extends PropertyContainer>> allowedTypes() {
        return Set.of(Client.class, Gamer.class);
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
