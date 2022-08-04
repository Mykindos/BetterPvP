package me.mykindos.betterpvp.clans.champions.builds.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

@BPvPListener
public class BuildListener implements Listener {

    private final GamerManager gamerManager;

    @Inject
    public BuildListener(GamerManager gamerManager) {
        this.gamerManager = gamerManager;
    }


    @EventHandler
    public void onOpenBuildManager(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            if(block == null) return;
            if (block.getType() == Material.ENCHANTING_TABLE) {
                Optional<Gamer> gamerOptional = gamerManager.getObject(e.getPlayer().getUniqueId());
                gamerOptional.ifPresent(gamer -> {
                    MenuManager.openMenu(e.getPlayer(), new ClassSelectionMenu(e.getPlayer(), gamer));
                    e.setCancelled(true);
                });
            }
        }
    }

}
