package me.mykindos.betterpvp.clans.clans.listeners;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.roles.RoleManager;
import me.mykindos.betterpvp.clans.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.utilities.UtilClans;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;
import java.util.Optional;

@BPvPListener
public class ClansShieldListener implements Listener {

    private final Clans clans;
    private final GamerManager gamerManager;
    private final RoleManager roleManager;

    @Inject
    public ClansShieldListener(Clans clans, GamerManager gamerManager, RoleManager roleManager) {
        this.clans = clans;
        this.gamerManager = gamerManager;
        this.roleManager = roleManager;
    }

    /**
     * No hand swapping!
     *
     * @param event the event
     */
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onJoinShield(PlayerJoinEvent event) {
        UtilServer.runTaskLater(clans, () -> giveShieldIfRequired(event.getPlayer()), 20);

    }

    @EventHandler
    public void onPickupGiveShield(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            UtilServer.runTaskLater(clans, () -> {
                if (player.getInventory().getItemInMainHand().getType() == item.getType()) {
                    giveShieldIfRequired(player);
                }
            }, 10);
        }
    }

    @EventHandler
    public void onInventoryMoveShield(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.PLAYER) {
            Player player = (Player) event.getInitiator().getViewers().get(0);
            if (player != null) {
                UtilServer.runTaskLater(clans, () -> {
                    if (player.getInventory().getItemInMainHand().getType() == event.getItem().getType()) {
                        giveShieldIfRequired(player);
                    }
                }, 10);

            }
        }
    }

    @EventHandler
    public void onItemSwap(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null) {
            UtilServer.runTaskLater(clans, () -> giveShieldIfRequired(player), 1);
        } else {
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler
    public void onPickupShield(EntityPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.SHIELD) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onClickOffhand(InventoryClickEvent event) {

        if (event.getClickedInventory() != null) {
            if (event.getCurrentItem() != null) {
                if (event.getCurrentItem().getType() == Material.SHIELD) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @UpdateEvent(delay = 1000)
    public void returnUnknownOffhands() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand.getType() != Material.SHIELD && offhand.getType() != Material.AIR) {
                ItemStack temp = player.getInventory().getItemInOffHand().clone();
                player.getInventory().setItemInOffHand(null);
                UtilItem.insert(player, temp);
            }

        });
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRoleChange(RoleChangeEvent event) {
        Player player = event.getPlayer();
        giveShieldIfRequired(player);
    }

    private void giveShieldIfRequired(Player player) {
        if (UtilClans.isUsableWithShield(player.getInventory().getItemInMainHand())) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
            if (gamerOptional.isPresent()) {
                Gamer gamer = gamerOptional.get();
                Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId());
                roleOptional.ifPresentOrElse(role -> {
                    RoleBuild build = gamer.getActiveBuilds().get(role.getName());
                    if (build != null) {
                        if (build.getActiveSkills().stream().anyMatch(s -> s instanceof ChannelSkill)) {
                            player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
                        }
                    }
                }, () -> {
                    // TODO legendary check
                    player.getInventory().setItemInOffHand(null);
                });

                // TODO
                //Weapon weapon = WeaponManager.getWeapon(e.getPlayer().getInventory().getItemInMainHand());
                //if (weapon != null && weapon instanceof ChannelWeapon) {
                //    e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
                //}
            }

        } else {
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler
    public void onDropOffhand(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.SHIELD) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSkillEquip(SkillEquipEvent event) {
        if (event.getSkill() instanceof ChannelSkill) {
            Player player = event.getPlayer();
            if (UtilClans.isUsableWithShield(player.getInventory().getItemInMainHand())) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
            }
        }
    }

}
