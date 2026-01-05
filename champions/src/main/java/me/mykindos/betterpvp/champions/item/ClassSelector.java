package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@ItemKey("champions:class_selector")
@Singleton
@FallbackItem(Material.SCULK_SHRIEKER)
public class ClassSelector extends BaseItem {

    @Inject
    public ClassSelector() {
        super("Class Selector", ItemStack.of(Material.SCULK_SHRIEKER), ItemGroup.BLOCK, ItemRarity.UNCOMMON);
        addBaseComponent(new DescriptionComponent(1,
                Component.text("Place and stand on this selector to change your class.")));
    }
}
