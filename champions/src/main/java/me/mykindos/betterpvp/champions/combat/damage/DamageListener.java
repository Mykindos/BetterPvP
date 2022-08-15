package me.mykindos.betterpvp.champions.combat.damage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
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
    public DamageListener(ItemDamageManager itemDamageManager) {
        this.itemDamageManager = itemDamageManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeaponDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!event.getReason().equals("")) return;

        Material material = player.getInventory().getItemInMainHand().getType();

        Optional<ItemDamageValue> damageValue = itemDamageManager.getObject(material.name());
        damageValue.ifPresentOrElse(itemDamageValue -> event.setDamage(itemDamageValue.getDamage()),
                () -> event.setDamage(event.getDamage() * 0.75));

    }
}
