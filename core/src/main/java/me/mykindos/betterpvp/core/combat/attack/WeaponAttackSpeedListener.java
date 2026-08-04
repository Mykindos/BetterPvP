package me.mykindos.betterpvp.core.combat.attack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Resolves a wielder's {@code ATTACK_SPEED} attribute to the attack speed of the weapon they are holding.
 * <p>
 * The client simulates held-item attribute modifiers locally and receives the player's own modifiers over the
 * wire, then draws the attack indicator from the total. Both sides therefore only agree while they see the
 * same item contribution, which is why {@code ItemInstanceView} keeps attack speed on the packet item.
 */
@BPvPListener
@Singleton
public class WeaponAttackSpeedListener implements Listener {

    private static final NamespacedKey ATTACK_SPEED_KEY = new NamespacedKey("betterpvp", "weapon-attack-speed");

    private final Core core;
    private final ItemFactory itemFactory;
    private final MeleeCombatSettings settings;

    @Inject
    private WeaponAttackSpeedListener(Core core, ItemFactory itemFactory, MeleeCombatSettings settings) {
        this.core = core;
        this.itemFactory = itemFactory;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // Deferred so the offset is measured against the base value ClientListener sets during this same event
        applyNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // Respawning rebuilds the player entity, taking every transient modifier with it
        applyNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        applyNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        applyNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        // Catches everything a held-item switch does not: reforging in place, drops, pickups, plugin writes.
        if (event.getSlot() != event.getPlayer().getInventory().getHeldItemSlot()) {
            return;
        }

        applyNextTick(event.getPlayer());
    }

    // These events fire before the inventory settles, so read the main hand on the following tick.
    private void applyNextTick(Player player) {
        UtilServer.runTaskLater(core, () -> {
            if (player.isOnline()) {
                apply(player);
            }
        }, 1L);
    }

    private void apply(Player player) {
        final AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(ATTACK_SPEED_KEY);
        if (!settings.isAttackCooldownEnabled()) {
            return; // Base value is already pinned high enough that a bonus would be meaningless
        }

        // Held weapons replace attack speed rather than adding to it. Vanilla weapons carry a large negative
        // modifier off a base of 4 - a netherite axe is -3.0 - so anything additive would leave a sword swinging
        // once every several seconds. Measure whatever the held item contributes and offset it away, which lands
        // on the target speed for vanilla and custom weapons alike.
        final double desired = MeleeCombatSettings.BASE_ATTACK_SPEED
                * (1 + getAttackSpeedBonus(player.getInventory().getItemInMainHand()));
        final double current = attribute.getValue();
        if (Math.abs(desired - current) < 1.0E-6) {
            return;
        }

        attribute.addTransientModifier(new AttributeModifier(ATTACK_SPEED_KEY,
                desired - current,
                AttributeModifier.Operation.ADD_NUMBER));
    }

    private double getAttackSpeedBonus(ItemStack itemStack) {
        return itemFactory.fromItemStack(itemStack)
                .flatMap(instance -> instance.getComponent(StatContainerComponent.class))
                .flatMap(container -> container.getStat(StatTypes.MELEE_ATTACK_SPEED))
                .map(ItemStat::getValue)
                .orElse(0.0);
    }
}
