package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.item.MushroomStew;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Singleton
public class HubInventoryService {

    private final RoleManager roleManager;
    private final ItemFactory itemFactory;

    @Inject
    public HubInventoryService(RoleManager roleManager, ItemFactory itemFactory) {
        this.roleManager = roleManager;
        this.itemFactory = itemFactory;
    }

    public void applyHubHotbar(Player player) {
        player.getInventory().clear();

        final ItemStack quickPlay = ItemView.builder()
                .material(Material.COMPASS)
                .customModelData(700)
                .displayName(Component.text("Server Select", NamedTextColor.GREEN))
                .build()
                .get();
        player.getInventory().setItem(4, quickPlay);
        player.updateInventory();
    }

    public void applyFfaLoadout(Player player) {
        player.getInventory().clear();

        roleManager.equipWeapons(player);

        final MushroomStew stew = Objects.requireNonNull(itemFactory.getItemRegistry().getItemByClass(MushroomStew.class));
        final ItemStack soups = itemFactory.create(stew).createItemStack();
        soups.setAmount(3);
        UtilItem.insert(player, soups);

        player.updateInventory();
    }

    private ItemStack createItem(Material material) {
        return itemFactory.create(itemFactory.getFallbackItem(new ItemStack(material))).createItemStack();
    }
}
