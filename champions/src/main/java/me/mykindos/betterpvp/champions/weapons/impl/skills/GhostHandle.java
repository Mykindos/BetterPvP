package me.mykindos.betterpvp.champions.weapons.impl.skills;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
@BPvPListener
public class GhostHandle extends Weapon implements Listener {

    @Inject
    protected GhostHandle(Champions plugin) {
        super(plugin, "ghost_handle");
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("The remnant of a thrown axe", NamedTextColor.WHITE));
        lore.add(Component.text("Only the handle remains", NamedTextColor.WHITE));
        return lore;
    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event) {
        if (this.matches(event.getCurrentItem())) {
            event.setCancelled(true);
        }
        if (this.matches(event.getCursor())) {
            event.setCancelled(true);
        }

        if (event.getAction() != InventoryAction.HOTBAR_SWAP) return;
        if (event.getClickedInventory() == null) return;
        final ItemStack hotbarItem = event.getClickedInventory().getItem(event.getHotbarButton());
        if (this.matches(hotbarItem)) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!this.matches(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
    }

    @EventHandler
    //prevent this item from dropping on death
    public void onPlayerDeath(PlayerDeathEvent event) {
        Iterator<ItemStack> iterator = event.getPlayer().getInventory().iterator();

        UtilInventory.remove(event.getPlayer(), this.getItemStack());
        while (iterator.hasNext()) {
            ItemStack current = iterator.next();
            if (this.matches(current)) {
                current.setAmount(0);
            }
        }
    }
}
