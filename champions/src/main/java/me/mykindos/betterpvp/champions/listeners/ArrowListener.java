package me.mykindos.betterpvp.champions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Objects;

@Singleton
@BPvPListener
public class ArrowListener implements Listener {

    @Inject
    @Config(path = "combat.crit-arrows-damage", defaultValue = "0.0")
    private double critArrowDamage;

    @Inject
    @Config(path = "combat.arrow-base-damage", defaultValue = "6.0")
    private double baseArrowDamage;

    private final HashMap<Arrow, Float> arrows = new HashMap<>();

    private final Champions champions;

    @Inject
    public ArrowListener(Champions champions) {
        this.champions = champions;
    }

    private boolean isArrow(Projectile projectile) {
        return projectile instanceof Arrow || projectile instanceof SpectralArrow;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onShootBow(EntityShootBowEvent event) {
        if (!isArrow((Projectile) event.getProjectile())) {
            return;
        }

        final AbstractArrow arrow = (AbstractArrow) event.getProjectile();
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        final String item = player.getInventory().getItemInMainHand().getType().name();
        arrow.setMetadata("ShotWith", new FixedMetadataValue(champions, item));
        arrow.setMetadata("Force", new FixedMetadataValue(champions, event.getForce()));
        if (critArrowDamage <= 0) {
            arrow.setCritical(false);
        }
        arrow.setDamage(baseArrowDamage);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileDelay(PreDamageEvent event) {
        final DamageEvent damageEvent = event.getDamageEvent();
        final Projectile projectile = damageEvent.getProjectile();
        if (projectile == null) {
            return;
        }

        if (isArrow(projectile)) {
            final AbstractArrow arrow = (AbstractArrow) projectile;
            final float force = (float) Objects.requireNonNull(projectile.getMetadata("Force").getFirst().value());
            arrow.remove();
            damageEvent.setDamage((arrow.getDamage() * force / 3) + (arrow.isCritical() ? critArrowDamage : 0.0));
        }

        damageEvent.setForceDamageDelay(0);
        damageEvent.setDamageDelay(0);
    }

    @EventHandler
    public void onUpdateLore(ItemUpdateLoreEvent event) {
        if (event.getItemStack().getType() == Material.ARROW) {
            event.getItemLore().clear();
            event.getItemLore().add(UtilMessage.deserialize("<reset>Damage: <green>%s", baseArrowDamage));
        }
    }

    /*
     * Removes arrows when they hit the ground, or a player
     */
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            UtilServer.runTaskLater(champions, () -> {
                arrow.remove();
                arrows.remove(arrow);
            }, 40L);
        }
    }

}
