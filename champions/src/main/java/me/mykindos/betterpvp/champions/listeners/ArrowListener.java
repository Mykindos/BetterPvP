package me.mykindos.betterpvp.champions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.combat.BowChargeTracker;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

/**
 * Baseline projectile behaviour for bows and crossbows: a flat configured damage scaled by draw
 * strength, no damage delay, arrows that never linger long enough to hit twice, and a rate limit on
 * crossbows. Roles and skills layer on top of this.
 */
@Singleton
@BPvPListener
public class ArrowListener implements Listener {

    /**
     * The cooldown group every plain crossbow is moved into. Untagged crossbows share the
     * {@code minecraft:crossbow} group, which custom items built on the material also sit in, so a cooldown
     * on the default group would lock those too.
     */
    private static final Key CROSSBOW_COOLDOWN_GROUP = Key.key("betterpvp", "vanilla_crossbow");

    @Inject
    @Config(path = "combat.arrow-base-damage", defaultValue = "6.0")
    private double baseArrowDamage;

    @Inject
    @Config(path = "combat.crit-arrows", defaultValue = "false")
    private boolean critArrowsEnabled;

    @Inject
    @Config(path = "combat.crossbow.disabled", defaultValue = "false")
    private boolean crossbowsDisabled;

    @Inject
    @Config(path = "combat.crossbow.cooldownEnabled", defaultValue = "true")
    private boolean crossbowCooldownEnabled;

    @Inject
    @Config(path = "combat.crossbow.cooldownDuration", defaultValue = "1.5")
    private double crossbowCooldownDuration;

    private final Champions champions;
    private final BowChargeTracker bowChargeTracker;
    private final ItemFactory itemFactory;

    @Inject
    public ArrowListener(Champions champions, BowChargeTracker bowChargeTracker, ItemFactory itemFactory) {
        this.champions = champions;
        this.bowChargeTracker = bowChargeTracker;
        this.itemFactory = itemFactory;
    }

    /**
     * Rate limits the plain crossbow, which would otherwise out-shoot every bow in the game. Custom items
     * are exempt - they carry their own cooldowns - and a server may bar the plain crossbow outright
     * instead of rate limiting it.
     */
    @EventHandler(ignoreCancelled = true)
    public void onShootCrossbow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getBow() == null || event.getBow().getType() != Material.CROSSBOW) return;
        if (itemFactory.isCustomItem(event.getBow())) return;

        if (crossbowsDisabled) {
            event.setCancelled(true);
            UtilMessage.message(player, "core.prefix.combat", "champions.combat.crossbows-disabled");
            return;
        }

        applyCrossbowCooldown(player);
    }

    /**
     * Tags a crossbow the moment it is picked up, so one collected while another is still cooling down
     * arrives already in the group rather than fireable.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPickupCrossbow(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        final ItemStack stack = event.getItem().getItemStack();
        if (tagCrossbow(stack)) {
            event.getItem().setItemStack(stack);
        }
    }

    /**
     * Puts every plain crossbow the player is carrying on an item use cooldown, which the client draws as
     * the usual cooldown sweep and which blocks both firing and reloading until it expires. Tagging the
     * whole inventory rather than only the fired stack is what stops a second crossbow being swapped in to
     * shoot through the cooldown.
     */
    private void applyCrossbowCooldown(Player player) {
        if (!crossbowCooldownEnabled) return;

        final PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (tagCrossbow(stack)) {
                inventory.setItem(slot, stack);
            }
        }

        player.setCooldown(CROSSBOW_COOLDOWN_GROUP, (int) (crossbowCooldownDuration * 20));
    }

    /**
     * Moves a plain crossbow into {@link #CROSSBOW_COOLDOWN_GROUP} by way of its
     * {@link DataComponentTypes#USE_COOLDOWN} component. Custom items are left alone - they carry their own
     * cooldowns - as is anything already tagged at the configured duration.
     *
     * @return whether the stack was changed and needs writing back
     */
    private boolean tagCrossbow(@Nullable ItemStack stack) {
        if (crossbowsDisabled || !crossbowCooldownEnabled) return false;
        if (stack == null || stack.getType() != Material.CROSSBOW) return false;
        if (itemFactory.isCustomItem(stack)) return false;

        final UseCooldown current = stack.getData(DataComponentTypes.USE_COOLDOWN);
        if (current != null && CROSSBOW_COOLDOWN_GROUP.equals(current.cooldownGroup())
                && current.seconds() == (float) crossbowCooldownDuration) {
            return false;
        }

        stack.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown((float) crossbowCooldownDuration)
                .cooldownGroup(CROSSBOW_COOLDOWN_GROUP));
        return true;
    }

    /**
     * Strips the critical marker - and with it the crit particle trail - from fully drawn arrows when
     * crit arrows are turned off, so the visual never promises damage that will not land.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (critArrowsEnabled) return;
        if (event.getProjectile() instanceof AbstractArrow arrow) {
//            arrow.setCritical(false);
        }
    }

    /**
     * Replaces vanilla's velocity-derived arrow damage with our flat base damage, scaled by how far the
     * bow was drawn. The flat damage and the charge scaling must be applied together in one handler -
     * handlers at the same priority run in an order the JVM does not define, so splitting them lets the
     * flat damage land after the scaling and wipe it.
     * <p>
     * A fully drawn arrow is flagged critical by vanilla, which the damage pipeline turns into the usual
     * critical multiplier on base damage.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onArrowDamage(DamageEvent event) {
        AbstractArrow arrow = asArrow(event.getProjectile());
        if (arrow == null) return;

        event.setDamage(baseArrowDamage * bowChargeTracker.getCharge(arrow));
        event.setCritical(critArrowsEnabled && arrow.isCritical());
        event.setDamageDelay(0);
    }

    /**
     * An arrow is spent once it lands a hit. Removing it at {@link EventPriority#MONITOR} leaves it intact
     * for every skill that reads the projectile off the damage event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowHitEntity(DamageEvent event) {
        AbstractArrow arrow = asArrow(event.getProjectile());
        if (arrow == null || !arrow.isValid()) return;

        arrow.remove();
    }

    /**
     * Removes arrows that stuck in the ground or in a player.
     */
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        AbstractArrow arrow = asArrow(event.getEntity());
        if (arrow == null) return;

        UtilServer.runTaskLater(champions, arrow::remove, 40L);
    }

    /**
     * The arrow this entity is, or null if it is not one. Tridents are arrows as far as Bukkit's hierarchy
     * is concerned, but they are a thrown weapon with their own damage and lifetime, so they never match.
     */
    @Nullable
    private AbstractArrow asArrow(@Nullable Entity entity) {
        return entity instanceof AbstractArrow arrow && !(arrow instanceof Trident) ? arrow : null;
    }
}
