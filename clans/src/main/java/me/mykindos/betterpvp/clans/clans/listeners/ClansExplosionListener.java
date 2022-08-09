package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

@BPvPListener
public class ClansExplosionListener extends ClanListener {


    @Inject
    @Config(path="clans.tnt.enabled", defaultValue = "false")
    private boolean tntEnabled;

    @Inject
    public ClansExplosionListener(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @EventHandler
    public void onTNTPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() == Material.TNT) {
            if (!tntEnabled) {
                UtilMessage.message(e.getPlayer(), "TNT", "TNT is disabled for the first 3 days of each season.");
                e.setCancelled(true);
            }
        }
    }

    /*
     * Prevents players from igniting TNT in Admin Protected Areas
     */
    @EventHandler
    public void preventTnTIgniting(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.TNT) return;
            Optional<Clan> clanOptional = clanManager.getClanByLocation(block.getLocation());
            clanOptional.ifPresent(clan -> {
                if (clan.isAdmin()) {
                    Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
                    gamerOptional.ifPresent(gamer -> {
                        if (!gamer.getClient().isAdministrating()) {
                            event.setCancelled(true);
                        }
                    });
                }
            });
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event){
        event.setCancelled(true);
    }
}
