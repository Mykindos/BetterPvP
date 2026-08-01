package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Not registered: it carries no {@code ItemKey}, so the item scan skips it and no
 * {@code champions:class_selector} exists at runtime. Classes are picked at the sculk shrieker itself, which
 * {@link me.mykindos.betterpvp.champions.champions.roles.listeners.ClassSelectorListener} drives off the block
 * type rather than this item.
 */
@Deprecated(forRemoval = true)
@Singleton
public class ClassSelector extends BaseItem {

    @Inject
    public ClassSelector() {
        super(translatableName("champions.item.class-selector.name"), ItemStack.of(Material.SCULK_SHRIEKER), ItemGroup.BLOCK, ItemRarity.UNCOMMON);
        addBaseComponent(DescriptionComponent.translatable(1, "champions.item.class-selector.lore"));
    }
}
