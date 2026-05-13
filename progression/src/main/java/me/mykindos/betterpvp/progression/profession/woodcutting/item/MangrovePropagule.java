package me.mykindos.betterpvp.progression.profession.woodcutting.item;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Singleton
@ItemKey("progression:mangrove_propagule")
@FallbackItem(value = Material.MANGROVE_PROPAGULE, keepRecipes = true)
public class MangrovePropagule extends VanillaItem {

    @Inject
    public MangrovePropagule() {
        super("Mangrove Propagule", Material.MANGROVE_PROPAGULE, ItemRarity.UNCOMMON);

        addBaseComponent(new DescriptionComponent(1,
                Component.text("Mangrove Trees grant more ")
                        .append(Component.text("Woodcutting Experience").color(NamedTextColor.AQUA))
                        .append(Component.text(" than any other tree."))
        ));

    }
}
