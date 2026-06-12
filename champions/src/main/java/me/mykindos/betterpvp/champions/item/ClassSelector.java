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
        super(translatableName("champions.item.class-selector.name"), ItemStack.of(Material.SCULK_SHRIEKER), ItemGroup.BLOCK, ItemRarity.UNCOMMON);
        addBaseComponent(DescriptionComponent.translatable(1, "champions.item.class-selector.lore"));
    }
}
