package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
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
public class WaterDamageAbility extends ItemAbility implements Listener {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;
    @EqualsAndHashCode.Exclude
    private final BaseItem heldItem;
    private double bonusDamage;

    public WaterDamageAbility(Champions champions, ItemFactory itemFactory, BaseItem heldItem) {
        super(new NamespacedKey(champions, "water_damage"),
                "Water Damage",
                "Increases damage dealt by melee attacks while in water by a flat amount.",
                TriggerTypes.PASSIVE);
        this.itemFactory = itemFactory;
        this.heldItem = heldItem;
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        // ignore
        return true;
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(DamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        // Add bonus damage if in water
        if (!damager.getLocation().getBlock().isLiquid()) {
            return;
        }

        itemFactory.fromItemStack(damager.getEquipment().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != heldItem) return; // Ensure the held item matches

            event.setDamage(event.getDamage() + bonusDamage);
        });
    }
} 