package me.mykindos.betterpvp.core.combat.hitbox;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import java.util.Objects;
import java.util.UUID;

@BPvPListener
public class HitboxHandler extends Manager<Hitbox> implements Listener {

    @Inject
    private Core core;

    public void clearAll() {
        for (Hitbox hitbox : getObjects().values()) {
            hitbox.remove();
        }
        getObjects().clear();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        getObject(event.getPlayer().getUniqueId()).ifPresent(hitbox -> {
            UtilServer.runTaskLater(core, hitbox::relocate, 1L);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        addObject(event.getPlayer().getUniqueId(), new Hitbox(event.getPlayer(), core));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        getObject(event.getPlayer().getUniqueId()).ifPresent(Hitbox::relocate);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        getObject(event.getPlayer().getUniqueId()).ifPresent(hitbox -> {
            removeObject(event.getPlayer().getUniqueId().toString());
            hitbox.remove();
        });
    }

    @EventHandler
    public void onClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) {
            return;
        }

        if (!interaction.getPersistentDataContainer().has(CoreNamespaceKeys.OWNER, CustomDataType.UUID)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction interaction)) {
            return;
        }

        if (!interaction.getPersistentDataContainer().has(CoreNamespaceKeys.OWNER, CustomDataType.UUID)) {
            return;
        }

        final LivingEntity damager = getDamagerEntity(event);
        final Projectile projectile = getProjectile(event);
        if (damager == null) {
            return;
        }

        UUID uuid = interaction.getPersistentDataContainer().get(CoreNamespaceKeys.OWNER, CustomDataType.UUID);
        final Player player = Bukkit.getPlayer(Objects.requireNonNull(uuid));
        if (player == null) {
            return;
        }

        final CustomDamageEvent cde = new CustomDamageEvent(
                player,
                damager,
                projectile,
                event.getCause(),
                event.getDamage(),
                true);
        UtilDamage.doCustomDamage(cde);
        event.setCancelled(true);
    }

    private Projectile getProjectile(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent ev)) {
            return null;
        }

        if ((ev.getDamager() instanceof Projectile)) {
            return (Projectile) ev.getDamager();
        }
        return null;
    }

    public static LivingEntity getDamagerEntity(EntityDamageEvent event) {

        if (!(event instanceof EntityDamageByEntityEvent ev)) {
            return null;
        }

        if ((ev.getDamager() instanceof LivingEntity)) {
            return (LivingEntity) ev.getDamager();
        }

        if (!(ev.getDamager() instanceof Projectile projectile)) {
            return null;
        }

        if (projectile.getShooter() == null) {
            return null;
        }
        if (!(projectile.getShooter() instanceof LivingEntity)) {
            return null;
        }
        return (LivingEntity) projectile.getShooter();
    }

}

