package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LifestealAbility extends ItemAbility implements Listener {

    @EqualsAndHashCode.Exclude
    private final BaseItem baseItem;
    private final Function<Player, Double> healFunction;
    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;

    public LifestealAbility(Champions champions, ItemFactory itemFactory, BaseItem baseItem, Function<Player, Double> healFunction) {
        super(new NamespacedKey(champions, "lifesteal"),
                "Lifesteal",
                "Heal for a portion of the damage dealt to an enemy from melee attacks.",
                TriggerTypes.PASSIVE);
        this.itemFactory = itemFactory;
        this.baseItem = baseItem;
        this.healFunction = healFunction;
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        // This is a passive ability, so we don't need to implement the invoke method
        // Healing is handled in the event listener below
        return true;
    }
    
    /**
     * Apply healing when the player damages an entity with the Scythe
     */
    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        itemFactory.fromItemStack(damager.getEquipment().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != baseItem) return; // Ensure the held item matches

            UtilPlayer.health(damager, healFunction.apply(damager));
        });
    }
} 