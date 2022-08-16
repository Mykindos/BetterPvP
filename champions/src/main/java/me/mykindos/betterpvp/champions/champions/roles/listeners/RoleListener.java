package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageDurabilityEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
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

import java.util.Optional;

@BPvPListener
public class RoleListener implements Listener {

    private final RoleManager roleManager;
    private final GamerManager gamerManager;
    private final BuildManager buildManager;
    private final CooldownManager cooldownManager;

    @Inject
    public RoleListener(RoleManager roleManager, GamerManager gamerManager, BuildManager buildManager, CooldownManager cooldownManager) {
        this.roleManager = roleManager;
        this.gamerManager = gamerManager;
        this.buildManager = buildManager;
        this.cooldownManager = cooldownManager;
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

            gamerManager.getObject(player.getUniqueId()).ifPresent(gamer -> {
                String roleProperty = role.name() + "_EQUIPPED";
                int timesEquipped = (int) gamer.getProperty(roleProperty).orElse(0) + 1;
                gamer.saveProperty(roleProperty, timesEquipped);
            });
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

    @EventHandler
    public void reduceDurability(CustomDamageDurabilityEvent event) {
        if (event.getCustomDamageEvent().getDamagee() instanceof Player player) {
            roleManager.getObject(player.getUniqueId()).ifPresent(role -> {
                if (role != Role.WARLOCK) {
                    double chance = (float) Role.WARLOCK.getChestplate().getMaxDurability() / (float) role.getChestplate().getMaxDurability();
                    if (UtilMath.randDouble(0, chance) >= 1) {
                        event.setDamageeTakeDurability(false);
                    }
                }
            });

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
        roleManager.getObject(event.getPlayer().getUniqueId()).ifPresent(role -> {
            if (event.getNewBuild().getRole() == role) {
                UtilMessage.message(player, equipMessage(player, role));
            }
        });
    }

    @EventHandler
    public void onDeleteBuild(DeleteBuildEvent event) {
        Player player = event.getPlayer();
        roleManager.getObject(event.getPlayer().getUniqueId()).ifPresent(role -> {
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
        Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId().toString());
        if (gamerBuildsOptional.isPresent()) {
            GamerBuilds builds = gamerBuildsOptional.get();

            RoleBuild build = builds.getActiveBuilds().get(role.getName());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void damageSound(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Optional<Role> roleOptional = roleManager.getObject(damagee.getUniqueId().toString());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            if (cooldownManager.add(damagee, "DamageSound", 0.7, false)) {
                switch (role) {
                    case KNIGHT ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0F, 0.7F);
                    case ASSASSIN ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 2.0F);
                    case GLADIATOR ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0F, 0.9F);
                    case RANGER ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.4F);
                    case PALADIN, WARLOCK ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.8F);
                }
            }
        }
    }

}
