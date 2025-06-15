package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ReverseKnockbackAbility extends ItemAbility implements Listener {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;
    @EqualsAndHashCode.Exclude
    private final BaseItem heldItem;
    private double knockbackMultiplier;
    private boolean bypassMinimum;

    public ReverseKnockbackAbility(Champions champions, ItemFactory itemFactory, BaseItem heldItem) {
        super(new NamespacedKey(champions, "reverse_knockback"),
                "Magnetic Pull",
                "Enemies hit by this weapon will be pulled inwards instead of pushed away.",
                TriggerType.PASSIVE);
        this.itemFactory = itemFactory;
        this.heldItem = heldItem;
        this.knockbackMultiplier = -1.0; // Default is to reverse knockback
        this.bypassMinimum = true;       // Default is to bypass minimum knockback
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        // This ability is passive and doesn't need direct invocation
        return true;
    }
    
    /**
     * Handle custom knockback for weapons with this ability
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKnockback(CustomKnockbackEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCustomDamageEvent().getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return; // Only apply to melee attacks
        }

        // Check if the weapon is being held
        itemFactory.fromItemStack(damager.getInventory().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != heldItem) return; // Ensure the held item matches

            // Apply custom knockback settings
            event.setCanBypassMinimum(bypassMinimum);
            event.setMultiplier(knockbackMultiplier);
        });
    }
} 