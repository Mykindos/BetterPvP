package me.mykindos.betterpvp.champions.combat;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.combat.events.PlayerCheckShieldEvent;
import me.mykindos.betterpvp.champions.utilities.UtilChampions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
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
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;
import java.util.Optional;

@BPvPListener
public class ShieldListener implements Listener {

    private final Champions champions;
    private final BuildManager buildManager;
    private final RoleManager roleManager;

    @Inject
    public ShieldListener(Champions champions, BuildManager buildManager, RoleManager roleManager) {
        this.champions = champions;
        this.buildManager = buildManager;
        this.roleManager = roleManager;
    }

    @EventHandler
    public void onJoinShield(PlayerJoinEvent event) {
        UtilServer.runTaskLater(champions, () -> giveShieldIfRequired(event.getPlayer()), 20);

    }

    @EventHandler
    public void onPickupGiveShield(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            UtilServer.runTaskLater(champions, () -> {
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
                UtilServer.runTaskLater(champions, () -> {
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
            UtilServer.runTaskLater(champions, () -> giveShieldIfRequired(player), 1);
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

    @UpdateEvent
    public void checkShields() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if(player.getInventory().getItemInOffHand().getType() == Material.SHIELD) return;
            var event = UtilServer.callEvent(new PlayerCheckShieldEvent(player));
            if(event.isShouldHaveShield()) {
                var shield = new ItemStack(Material.SHIELD);
                var itemMeta = shield.getItemMeta();
                itemMeta.setCustomModelData(1);
                shield.setItemMeta(itemMeta);
                player.getInventory().setItemInOffHand(shield);
            }
        });
    }

    private void giveShieldIfRequired(Player player) {
        if (UtilChampions.isUsableWithShield(player.getInventory().getItemInMainHand())) {
            Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId());
            if (gamerBuildsOptional.isPresent()) {
                GamerBuilds builds = gamerBuildsOptional.get();
                Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId());
                roleOptional.ifPresentOrElse(role -> {
                    RoleBuild build = builds.getActiveBuilds().get(role.getName());
                    if (build != null) {
                        if (build.getActiveSkills().stream().anyMatch(s -> s instanceof ChannelSkill)) {
                            var shield = new ItemStack(Material.SHIELD);
                            var itemMeta = shield.getItemMeta();
                            itemMeta.setCustomModelData(1);
                            shield.setItemMeta(itemMeta);
                            player.getInventory().setItemInOffHand(shield);
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
            if (UtilChampions.isUsableWithShield(player.getInventory().getItemInMainHand())) {
                var shield = new ItemStack(Material.SHIELD);
                var itemMeta = shield.getItemMeta();
                itemMeta.setCustomModelData(1);
                shield.setItemMeta(itemMeta);
                player.getInventory().setItemInOffHand(shield);
            }
        }
    }

}
