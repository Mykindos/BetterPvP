package me.mykindos.betterpvp.clans.champions.roles.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.roles.RoleManager;
import me.mykindos.betterpvp.clans.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageDurabilityEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;

@BPvPListener
public class RoleListener implements Listener {

    private final RoleManager roleManager;
    private final GamerManager gamerManager;

    @Inject
    public RoleListener(RoleManager roleManager, GamerManager gamerManager) {
        this.roleManager = roleManager;
        this.gamerManager = gamerManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRoleChange(RoleChangeEvent event) {

        Player player = event.getPlayer();
        Role role = event.getRole();

        if (role == null) {
            UtilMessage.message(player, "Class", "Armor Class: " + ChatColor.GREEN + "None");
        } else {
            roleManager.addObject(player.getUniqueId().toString(), role);
            UtilMessage.message(player, "Class", "You equipped " + ChatColor.GREEN + role.getName());
            UtilMessage.message(player, equipMessage(player, role));
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {

            if (effect.getType().getName().equals("POISON")
                    || effect.getType().getName().equals("CONFUSION")
                    || effect.getType().getName().equals("BLINDNESS")) {

                continue;
            }
            player.removePotionEffect(effect.getType());

        }


        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 2.0F, 1.09F);

    }

    @UpdateEvent(delay = 250)
    public void checkRoles() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!checkNoRoleEquipped(player)) {
                checkEquippedRole(player);
            } else {
                equipRole(player, null);
            }

        }
    }

    private void checkEquippedRole(Player player) {
        EntityEquipment equipment = player.getEquipment();
        for (Role role : Role.values()) {
            if (equipment.getHelmet().getType() == role.getHelmet()
                    && equipment.getChestplate().getType() == role.getChestplate()
                    && equipment.getLeggings().getType() == role.getLeggings()
                    && equipment.getBoots().getType() == role.getBoots()) {
                equipRole(player, role);
                return;
            }

        }

        equipRole(player, null);
    }

    @UpdateEvent(delay = 500)
    public void checkRoleBuffs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId().toString());
            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                if (role == Role.ASSASSIN) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                }
            }
        }
    }

    @EventHandler
    public void reduceDurability(CustomDamageDurabilityEvent event) {
        if (event.getCustomDamageEvent().getDamagee() instanceof Player player) {
            Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId());
            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                if (role != Role.WARLOCK) {
                    double chance = (float) Role.WARLOCK.getChestplate().getMaxDurability() / (float) role.getChestplate().getMaxDurability();
                    if (UtilMath.randDouble(0, chance) >= 1) {
                        event.setDamageeTakeDurability(false);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onGoldSwordDamage(CustomDamageDurabilityEvent event) {
        if (event.getCustomDamageEvent().getDamager() instanceof Player player) {
            Material weapon = player.getInventory().getItemInMainHand().getType();
            if (weapon == Material.GOLDEN_SWORD) {
                if (UtilMath.randomInt(0, 10) <= 6) {
                    event.setDamagerTakeDurability(false);
                }
            }
        }
    }

    @EventHandler
    public void onApplyBuild(ApplyBuildEvent event) {
        Player player = event.getPlayer();
        Optional<Role> roleOptional = roleManager.getObject(event.getPlayer().getUniqueId());
        roleOptional.ifPresent(role -> {
            if (event.getNewBuild().getRole() == role) {
                UtilMessage.message(player, equipMessage(player, role));
            }
        });

    }

    @EventHandler
    public void onDeleteBuild(DeleteBuildEvent event) {
        Player player = event.getPlayer();
        Optional<Role> roleOptional = roleManager.getObject(event.getPlayer().getUniqueId());
        roleOptional.ifPresent(role -> {
            if (event.getRoleBuild().getRole() == role) {
                UtilMessage.message(player, equipMessage(player, role));
            }
        });
    }

    private void equipRole(Player player, Role role) {
        if (role == null) {
            if (roleManager.getObjects().containsKey(player.getUniqueId().toString())) {
                roleManager.removeObject(player.getUniqueId().toString());
                UtilServer.callEvent(new RoleChangeEvent(player, null));
            }
            return;
        }

        Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId().toString());
        if (roleOptional.isEmpty() || roleOptional.get() != role) {
            UtilServer.callEvent(new RoleChangeEvent(player, role));
        }
    }

    private boolean checkNoRoleEquipped(Player player) {
        for (ItemStack armour : player.getEquipment().getArmorContents()) {
            if (armour == null || armour.getType() == Material.AIR) {

                return true;
            }
        }
        return false;
    }

    public String[] equipMessage(Player player, Role role) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if (gamerOptional.isPresent()) {
            Gamer gamer = gamerOptional.get();

            RoleBuild build = gamer.getActiveBuilds().get(role.getName());
            if (build != null) {

                String sword = build.getSwordSkill() == null ? "" : build.getSwordSkill().getString();
                String axe = build.getAxeSkill() == null ? "" : build.getAxeSkill().getString();
                String bow = build.getBow() == null ? "" : build.getBow().getString();
                String passivea = build.getPassiveA() == null ? "" : build.getPassiveA().getString();
                String passiveb = build.getPassiveB() == null ? "" : build.getPassiveB().getString();
                String global = build.getGlobal() == null ? "" : build.getGlobal().getString();
                return new String[]{
                        ChatColor.GREEN + "Sword: " + ChatColor.WHITE + sword,
                        ChatColor.GREEN + "Axe: " + ChatColor.WHITE + axe,
                        ChatColor.GREEN + "Bow: " + ChatColor.WHITE + bow,
                        ChatColor.GREEN + "Passive A: " + ChatColor.WHITE + passivea,
                        ChatColor.GREEN + "Passive B: " + ChatColor.WHITE + passiveb,
                        ChatColor.GREEN + "Global: " + ChatColor.WHITE + global
                };
            }
        }

        return new String[]{};

    }

}
