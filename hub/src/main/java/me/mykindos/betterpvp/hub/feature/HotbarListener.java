package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;

@BPvPListener
@Singleton
public class HotbarListener implements Listener {


    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        final Player player = Objects.requireNonNull(event.getClient().getGamer().getPlayer());
        player.getInventory().clear();

        final ItemStack quickPlay = ItemView.builder()
                .material(Material.STICK)
                .customModelData(700)
                .displayName(Component.text("Quick Play", NamedTextColor.GREEN))
                .build()
                .get();
        player.getInventory().setItem(4, quickPlay);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (GameMode.CREATIVE.equals(event.getWhoClicked().getGameMode())) {
            return;
        }

        if (event.getClickedInventory() instanceof PlayerInventory) {
            event.setCancelled(true); // todo: maybe allow for ffa to work?
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (GameMode.CREATIVE.equals(event.getPlayer().getGameMode())) {
            return;
        }
        event.setCancelled(true); // todo: maybe allow for ffa to work?
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }

        final ItemStack item = Objects.requireNonNull(event.getItem());
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return;
        }

        final int customModelData = item.getItemMeta().getCustomModelData();
        switch (customModelData) {
            case 700:
                // todo: implement opening game selector
                event.getPlayer().sendMessage("Quick Play");
                break;
        }
    }


}
