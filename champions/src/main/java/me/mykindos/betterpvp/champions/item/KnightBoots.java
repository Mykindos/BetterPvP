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
@ItemKey("champions:knight_boots")
@FallbackItem(value = Material.IRON_BOOTS, keepRecipes = true)
public class KnightBoots extends ArmorItem {

    @Inject
    private KnightBoots(Champions champions) {
        super(champions, translatableName("champions.item.knight-boots.name"), ItemStack.of(Material.IRON_BOOTS), ItemRarity.COMMON);
        addBaseComponent(new RoleArmorComponent(Role.KNIGHT));
    }
}
