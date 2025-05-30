package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

@Singleton
public class CombatCategory extends AchievementCategory {
    @Inject
    public CombatCategory() {
        super(AchievementCategories.COMBAT_CATEGORY);
    }

    /**
     * Gets a mini-message formatted title for this category via {@link UtilMessage#deserialize(String)}
     *
     * @return the formatted title
     */


    @Override
    public Set<Class<? extends PropertyContainer>> allowedTypes() {
        return Set.of(Client.class, Gamer.class);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Component.text("Combat"))
                .lore(
                        List.of(Component.text("Combat related achievements"))
                )
                .material(Material.IRON_SWORD)
                .build();
    }
}
