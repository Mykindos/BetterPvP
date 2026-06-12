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
        super(translatableName("champions.item.build-editor.name"), ItemStack.of(Material.ENCHANTING_TABLE), ItemGroup.BLOCK, ItemRarity.UNCOMMON);
        addBaseComponent(DescriptionComponent.translatable(1, "champions.item.build-editor.lore"));
    }
}
