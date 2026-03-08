package me.mykindos.betterpvp.champions.champions.commands.menu;

import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.item.MushroomStew;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KitButton extends SimpleItem {

    private final Role role;
    private final RoleManager roleManager;
    private final ItemFactory itemFactory;
    private final boolean weapons;

    public KitButton(ItemView item, Role role, RoleManager roleManager, ItemFactory itemFactory, boolean weapons) {
        super(item);
        this.role = role;
        this.roleManager = roleManager;
        this.itemFactory = itemFactory;
        this.weapons = weapons;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        player.closeInventory();

        roleManager.equipRole(player, role);
        if (weapons) {
            roleManager.equipWeapons(player);
            final MushroomStew stew = Objects.requireNonNull(itemFactory.getItemRegistry().getItemByClass(MushroomStew.class));
            final ItemStack itemStack = itemFactory.create(stew).createItemStack();
            itemStack.setAmount(16);
            UtilItem.insert(player, itemStack);
        }
    }

}
