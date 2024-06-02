package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

@Singleton
@BPvPListener
public class ClanFarmingListener implements Listener {

    private final ClientManager clientManager;
    private final ClanManager clanManager;

    @Config(path = "clans.farming.baseY", defaultValue = "60")
    @Inject
    private int baseFarmingY;

    @Config(path = "clans.farming.baseFarmingLevels", defaultValue = "5")
    @Inject
    private int baseFarmingLevels;

    @Config(path = "clans.farming.allowOfflineGrowing", defaultValue = "false")
    @Inject
    private boolean offlineGrowing;

    @Inject
    public ClanFarmingListener(ClientManager clientManager, ClanManager clanManager) {
        this.clientManager = clientManager;
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!UtilBlock.isSeed(block.getType()) && !UtilBlock.isCultivation(block.getType())) {
            return;
        }

        Client client = clientManager.search().online(event.getPlayer());
        if (client != null && client.isAdministrating()) {
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByLocation(block.getLocation());
        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();

            int minY = baseFarmingY - baseFarmingLevels - ClanPerkManager.getInstance().getTotalFarmingLevels(clan);

            if(block.getY() < minY || block.getY() > baseFarmingY) {
                UtilMessage.simpleMessage(event.getPlayer(), "Clans", "Your clan can only cultivate between <green>%d</green> and <green>%d</green> Y.", minY, baseFarmingY);
                event.setCancelled(true);
            }
        } else {
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You cannot cultivate in the wilderness.");
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !event.getAction().isRightClick() || event.useItemInHand() == Event.Result.DENY) {
            return; // Only main hand and right click
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if(block.getType() != Material.FARMLAND) {
            return;
        }

        if(!UtilBlock.isSeed(event.getPlayer().getInventory().getItemInMainHand().getType())) {
            return;
        }

        Client client = clientManager.search().online(event.getPlayer());
        if (client != null && client.isAdministrating()) {
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByLocation(block.getLocation());
        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();

            int minY = baseFarmingY - baseFarmingLevels - ClanPerkManager.getInstance().getTotalFarmingLevels(clan);

            if(block.getY() < minY || block.getY() > baseFarmingY) {
                UtilMessage.simpleMessage(event.getPlayer(), "Clans", "Your clan can only cultivate between <green>%d</green> and <green>%d</green> Y.", minY, baseFarmingY);
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onGrow(BlockGrowEvent event) {
        if (offlineGrowing) {
            return;
        }

        Material type = event.getNewState().getType();
        if(type == Material.SUGAR_CANE || type.name().contains("MELON") || type.name().contains("PUMPKIN")) {
            clanManager.getClanByLocation(event.getBlock().getLocation()).ifPresent(clan -> {
                if(!clan.isOnline()) {
                    event.setCancelled(true);
                }
            });

        }


    }
}
