package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * A reusable ability that prevents fall damage when the player is holding an item with this ability.
 */
@Singleton
@BPvPListener
@EqualsAndHashCode(callSuper = false)
public class FeatherFeetAbility extends ItemAbility implements Listener {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;

    @Inject
    public FeatherFeetAbility(ItemFactory itemFactory) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "feather_feet"),
                "Feather Feet",
                "Prevents all incoming damage from falling while holding this item.",
                TriggerTypes.PASSIVE); // Passive ability
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        // This is a passive ability, so no active invocation
        return true;
    }

    /**
     * Cancels fall damage when a player is holding an item with this ability
     */
    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.AIR) {
            itemFactory.fromItemStack(mainHand).ifPresent(instance -> {
                Optional<AbilityContainerComponent> container = instance.getComponent(AbilityContainerComponent.class);
                if (container.isPresent() && container.get().getContainer().stream().anyMatch(ability -> ability instanceof FeatherFeetAbility)) {
                    event.setCancelled(true);
                    return;
                }
            });
        }

        // Check off hand (optional functionality)
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() != Material.AIR) {
            itemFactory.fromItemStack(offHand).ifPresent(instance -> {
                Optional<AbilityContainerComponent> container = instance.getComponent(AbilityContainerComponent.class);
                if (container.isPresent() && container.get().getContainer().stream().anyMatch(ability -> ability instanceof FeatherFeetAbility)) {
                    event.setCancelled(true);
                }
            });
        }
    }
} 