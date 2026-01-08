package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.RoleSoundProvider;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathMessageEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Function;

@Singleton
@BPvPListener
public class RoleListener implements Listener {

    private final ItemFactory itemFactory;
    private final RoleManager roleManager;
    private final ClientManager clientManager;
    private final BuildManager buildManager;
    private final RoleSoundProvider soundProvider;

    @Inject
    public RoleListener(ItemFactory itemFactory, RoleManager roleManager, ClientManager clientManager, BuildManager buildManager, RoleSoundProvider soundProvider) {
        this.itemFactory = itemFactory;
        this.roleManager = roleManager;
        this.clientManager = clientManager;
        this.buildManager = buildManager;
        this.soundProvider = soundProvider;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        roleManager.cleanUp(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        roleManager.populate(event.getPlayer());
        equipMessage(event.getPlayer(), roleManager.getRole(event.getPlayer()));
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity)  {
            roleManager.cleanUp(livingEntity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoleChange(RoleChangeEvent event) {
        final LivingEntity livingEntity = event.getLivingEntity();
        livingEntity.getWorld().playSound(livingEntity.getLocation(), Sound.ENTITY_HORSE_ARMOR, 2.0F, 1.09F);

        if (!(livingEntity instanceof Player player)) {
            return;
        }

        Role role = event.getRole();
        if (role == null) {
            UtilMessage.simpleMessage(player, "Class", "Armor Class: <green>None");
        } else {
            UtilMessage.simpleMessage(player, "Class", "You equipped <green>%s", role.getName());
            UtilMessage.message(player, equipMessage(player, role));
        }
    }

    @EventHandler
    public void onApplyBuild(ApplyBuildEvent event) {
        Player player = event.getPlayer();
        final Role role = roleManager.getRole(player);
        if (event.getNewBuild().getRole() == role) {
            UtilMessage.message(player, equipMessage(player, role));
        }
    }

    @EventHandler
    public void onDeleteBuild(DeleteBuildEvent event) {
        Player player = event.getPlayer();
        final Role role = roleManager.getRole(player);
        if (event.getRoleBuild().getRole() == role) {
            UtilMessage.message(player, equipMessage(player, role));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void damageSound(DamageEvent pre) {
        pre.setSoundProvider(soundProvider);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getBow() != null && itemFactory.isCustomItem(event.getBow())) {
            return; // custom bow
        }

        final LivingEntity livingEntity = event.getEntity();
        if (UtilBlock.isInLiquid(livingEntity)) {
            UtilMessage.message(livingEntity, "Bow", "You cannot shoot a bow in liquid.");
            event.setCancelled(true);
            return;
        }

        final Role role = roleManager.getRole(livingEntity).orElse(null);
        if (role != Role.ASSASSIN && role != Role.RANGER) {
            UtilMessage.message(livingEntity, "Bow", "You can't shoot a bow without Assassin or Ranger equipped.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(CustomDeathMessageEvent event) {
        final Function<LivingEntity, Component> def = event.getNameFormat();
        event.setNameFormat(entity -> {
            Component name = def.apply(entity);
            final Optional<Role> role = roleManager.getRole(entity);
            if (role.isPresent()) {
                final TextComponent prefix = Component.text(role.get().getPrefix() + ".", NamedTextColor.GREEN);
                name = Component.join(JoinConfiguration.noSeparators(), prefix, name);
            }
            return name;
        });
    }

    @EventHandler
    public void onArmourChange(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        Gamer gamer = clientManager.search().online(player).getGamer();

        if (UtilItem.isArmour(mainhand.getType())) {
            Material armorType = player.getInventory().getItem(mainhand.getType().getEquipmentSlot()).getType();

            if (armorType != Material.AIR && gamer.isInCombat()) {
                UtilMessage.message(player, "Class", "You cannot hotswap armor while in combat.");
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    public Component equipMessage(Player player, Role role) {
        Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId().toString());
        if (gamerBuildsOptional.isPresent()) {
            GamerBuilds builds = gamerBuildsOptional.get();

            RoleBuild build = builds.getActiveBuilds().get(role.getName());
            if (build != null) {
                return build.getBuildComponent();
            }
        }
        return Component.empty();
    }

}


