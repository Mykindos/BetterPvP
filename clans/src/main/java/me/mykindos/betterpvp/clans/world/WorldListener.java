package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.events.SettingsUpdatedEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
public class WorldListener implements Listener {

    private final GamerManager gamerManager;
    private final ClanManager clanManager;

    @Inject
    public WorldListener(GamerManager gamerManager, ClanManager clanManager) {
        this.gamerManager = gamerManager;
        this.clanManager = clanManager;
    }

    /*
     * Stops players from lighting fires on stuff like grass, wood, etc.
     * Helps keep the map clean
     */
    @EventHandler
    public void blockFlint(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.FLINT_AND_STEEL) {
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null && clickedBlock.getType() != Material.TNT && clickedBlock.getType() != Material.NETHERRACK) {
                    UtilMessage.message(event.getPlayer(), "Flint and Steel", "You may not use Flint and Steel on this block type!");
                    event.setCancelled(true);
                }
            }
        }
    }

    /*
     * Stops players from filling buckets with water or lava, and also breaks the bucket.
     */
    @EventHandler
    public void handleBucket(PlayerBucketFillEvent event) {
        event.setCancelled(true);
        UtilMessage.message(event.getPlayer(), "Game", "Your " + ChatColor.YELLOW + "Bucket" + ChatColor.GRAY + " broke!");
        ItemStack replacement = new ItemStack(Material.IRON_INGOT, event.getPlayer().getInventory().getItemInMainHand().getAmount() * 3);
        event.getPlayer().getInventory().setItemInMainHand(replacement);
    }

    /*
     * Stops leaf decay in admin clan territory
     */
    @EventHandler
    public void stopLeafDecay(LeavesDecayEvent event) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            event.setCancelled(true);
            return;
        }
        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
        clanOptional.ifPresent(clan -> {
            if (clan.isAdmin()) {
                event.setCancelled(true);
            }
        });

    }

    /*
     * Stops players from placing items such a levers and buttons on the outside of peoples bases
     * This is required, as previously, players could open the doors to an enemy base.
     */
    @EventHandler
    public void onAttachablePlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.LEVER || event.getBlock().getType().name().contains("_BUTTON")) {
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlockAgainst().getLocation());
            clanOptional.ifPresent(clan -> {
                Optional<Clan> playerClanOption = clanManager.getClanByPlayer(event.getPlayer());
                if (!playerClanOption.equals(clanOptional)) {
                    event.setCancelled(true);
                }
            });

        }
    }

    /*
     * Prevent fall damage when landing on wool or sponge
     */
    @EventHandler
    public void onSafeFall(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (UtilBlock.getBlockUnder(player.getLocation()).getType().name().contains("SPONGE")
                        || UtilBlock.getBlockUnder(player.getLocation()).getType().name().contains("WOOL")) {
                    e.setCancelled(true);
                }
            }
        }
    }


    /*
     * Players were creating chest rooms at sky limit, making them a lot harder to raid.
     * This requires all forms of item storage to be placed below 200y.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        gamerOptional.ifPresent(gamer -> {
            int blocksBroken = (int) (gamer.getProperty(GamerProperty.BLOCKS_PLACED.toString()).orElse(0)) + 1;
            gamer.putProperty(GamerProperty.BLOCKS_PLACED.toString(), blocksBroken);
            UtilServer.callEvent(new SettingsUpdatedEvent(player, gamer.getClient(), GamerProperty.BLOCKS_PLACED.toString()));
        });

        BlockState state = block.getState();
        if(state instanceof Container) {
            if (block.getLocation().getY() > 200) {
                UtilMessage.message(player, "Restriction", "You can only place chests lower than 200Y!");
                event.setCancelled(true);
            }
        }

    }

}
