package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.component.armor.RoleArmorComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:mage_chestplate")
@FallbackItem(value = Material.GOLDEN_CHESTPLATE, keepRecipes = true)
public class MageChestplate extends ArmorItem {

    @Inject
    private MageChestplate(Champions champions) {
        super(champions, translatableName("champions.item.mage-chestplate.name"), ItemStack.of(Material.GOLDEN_CHESTPLATE), ItemRarity.COMMON);
        addBaseComponent(new RoleArmorComponent(Role.MAGE));
    }
}
