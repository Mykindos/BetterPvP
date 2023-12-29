package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Optional;
import java.util.function.Function;

@BPvPListener
public class RoleListener implements Listener {

    private final RoleManager roleManager;
    private final ClientManager clientManager;
    private final BuildManager buildManager;
    private final CooldownManager cooldownManager;

    @Inject
    public RoleListener(RoleManager roleManager, ClientManager clientManager, BuildManager buildManager, CooldownManager cooldownManager) {
        this.roleManager = roleManager;
        this.clientManager = clientManager;
        this.buildManager = buildManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        roleManager.removeObject(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRoleChange(RoleChangeEvent event) {

        Player player = event.getPlayer();
        Role role = event.getRole();

        if (role == null) {
            UtilMessage.simpleMessage(player, "Class", "Armor Class: <green>None");
        } else {
            roleManager.addObject(player.getUniqueId().toString(), role);
            UtilMessage.simpleMessage(player, "Class", "You equipped <green>%s", role.getName());
            UtilMessage.message(player, equipMessage(player, role));

            final Gamer gamer = clientManager.search().online(player).getGamer();
            String roleProperty = role.name() + "_EQUIPPED";
            int timesEquipped = (int) gamer.getProperty(roleProperty).orElse(0) + 1;
            gamer.saveProperty(roleProperty, timesEquipped);
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
                final Optional<Role> previous = roleManager.getObject(player.getUniqueId().toString());
                roleManager.removeObject(player.getUniqueId().toString());
                UtilServer.callEvent(new RoleChangeEvent(player, null, previous.orElse(null)));
            }
            return;
        }

        Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId().toString());
        if (roleOptional.isEmpty() || roleOptional.get() != role) {
            UtilServer.callEvent(new RoleChangeEvent(player, role, roleOptional.orElse(null)));
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

    public Component[] equipMessage(Player player, Role role) {
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
                return new Component[]{
                        Component.text("Sword: ", NamedTextColor.GREEN).append(Component.text(sword, NamedTextColor.WHITE)),
                        Component.text("Axe: ", NamedTextColor.GREEN).append(Component.text(axe, NamedTextColor.WHITE)),
                        Component.text("Bow: ", NamedTextColor.GREEN).append(Component.text(bow, NamedTextColor.WHITE)),
                        Component.text("Passive A: ", NamedTextColor.GREEN).append(Component.text(passivea, NamedTextColor.WHITE)),
                        Component.text("Passive B: ", NamedTextColor.GREEN).append(Component.text(passiveb, NamedTextColor.WHITE)),
                        Component.text("Global: ", NamedTextColor.GREEN).append(Component.text(global, NamedTextColor.WHITE))
                };
            }
        }

        return new Component[]{};
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void damageSound(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Optional<Role> roleOptional = roleManager.getObject(damagee.getUniqueId().toString());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
                switch (role) {
                    case KNIGHT ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0F, 0.7F);
                    case ASSASSIN ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 2.0F);
                    case BRUTE ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0F, 0.9F);
                    case RANGER ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.4F);
                    case MAGE ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.8F);
                    case WARLOCK ->
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0F, 0.6F);
                }
        }
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.message(player, "Bow", "You can't shoot a bow in water.");
            event.setCancelled(true);
            return;
        }

        roleManager.getObject(player.getUniqueId()).ifPresentOrElse(role -> {
            if (role != Role.ASSASSIN && role != Role.RANGER) {
                UtilMessage.message(player, "Bow", "You can't shoot a bow without Assassin or Ranger equipped.");
                event.setCancelled(true);
            }
        }, () -> {
            UtilMessage.message(player, "Bow", "You can't shoot a bow without Assassin or Ranger equipped.");
            event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(CustomDeathEvent event) {
        final Function<LivingEntity, Component> def = event.getNameFormat();
        event.setNameFormat(entity -> {
            Component name = def.apply(entity);
            if (entity instanceof Player player) {
                final Optional<Role> role = roleManager.getObject(player.getUniqueId());
                if (role.isPresent()) {
                    final TextComponent prefix = Component.text(role.get().getPrefix() + ".", NamedTextColor.GREEN);
                    name = Component.join(JoinConfiguration.noSeparators(), prefix, name);
                }
            }
            return name;
        });
    }

}
