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
@ItemKey("champions:assassin_leggings")
@FallbackItem(value = Material.LEATHER_LEGGINGS, keepRecipes = true)
public class AssassinLeggings extends ArmorItem {

    @Inject
    private AssassinLeggings(Champions champions) {
        super(champions, translatableName("champions.item.assassin-leggings.name"), ItemStack.of(Material.LEATHER_LEGGINGS), ItemRarity.COMMON);
        addBaseComponent(new RoleArmorComponent(Role.ASSASSIN));
    }
}
