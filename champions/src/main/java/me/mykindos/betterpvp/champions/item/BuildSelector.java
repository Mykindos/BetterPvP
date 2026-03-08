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

@ItemKey("champions:build_editor")
@Singleton
@FallbackItem(Material.ENCHANTING_TABLE)
public class BuildSelector extends BaseItem {

    @Inject
    public BuildSelector() {
        super("Build Editor", ItemStack.of(Material.ENCHANTING_TABLE), ItemGroup.BLOCK, ItemRarity.UNCOMMON);
        addBaseComponent(new DescriptionComponent(1,
                Component.text("Place and click on this selector to edit your build.")));
    }
}
