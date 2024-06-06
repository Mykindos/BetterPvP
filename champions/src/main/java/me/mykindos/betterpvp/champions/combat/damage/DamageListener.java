package me.mykindos.betterpvp.champions.combat.damage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Optional;

@Singleton
@BPvPListener
public class DamageListener implements Listener {

    private final ItemDamageManager itemDamageManager;

    @Inject
    @Config(path = "damage.multiplier.fall", defaultValue = "0.8")
    private double fallDamageMultiplier;

    @Inject
    public DamageListener(ItemDamageManager itemDamageManager) {
        this.itemDamageManager = itemDamageManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeaponDamage(PreCustomDamageEvent preEvent) {
        if (preEvent.isCancelled()) return;
        CustomDamageEvent event = preEvent.getCustomDamageEvent();
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.hasReason()) return; // Skip custom damage reasoned events

        Material material = player.getInventory().getItemInMainHand().getType();

        Optional<ItemDamageValue> damageValue = itemDamageManager.getObject(material.name());
        final double damage = damageValue.map(ItemDamageValue::getDamage).orElse(0.5 * event.getDamage());
        event.setDamage(damage);
        event.setRawDamage(damage);
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onFallDamage(PreCustomDamageEvent event) {
        if(event.isCancelled()) return;
        CustomDamageEvent customDamageEvent = event.getCustomDamageEvent();

        if(customDamageEvent.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if(customDamageEvent.getDamagee() instanceof Player) return;

        customDamageEvent.setDamage(customDamageEvent.getDamage() * fallDamageMultiplier);

    }

}
