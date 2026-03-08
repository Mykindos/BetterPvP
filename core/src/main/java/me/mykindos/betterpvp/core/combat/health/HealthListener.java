package me.mykindos.betterpvp.core.combat.health;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.core.item.armor.ArmorEquipEvent;
import me.mykindos.betterpvp.core.item.armor.ArmorUnequipEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

@Singleton
@BPvPListener
public class HealthListener implements Listener {

    private final EntityHealthService entityHealthService;

    @Inject
    private HealthListener(EntityHealthService entityHealthService) {
        this.entityHealthService = entityHealthService;
    }

    @EventHandler
    void onArmorLiving(EntityEquipmentChangedEvent event) {
        if (!(event instanceof Player)) {
            updateHealth(event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onEquipPlayer(ArmorEquipEvent event) {
        updateHealth(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onUnequipPlayer(ArmorUnequipEvent event) {
        updateHealth(event.getPlayer());
    }

    @EventHandler
    void onAdd(CreatureSpawnEvent event) {
        updateHealth(event.getEntity());
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        updateHealth(event.getPlayer());
    }

    private void updateHealth(LivingEntity entity) {
        if (entity instanceof ArmorStand) {
            return;
        }

        final AttributeInstance attribute = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH));
        final double previousPercentage = entity.getHealth() / attribute.getValue();
        if (previousPercentage == 0) {
            return; // We don't want to update health if the entity is already dead
        }

        // Update max health
        final double defaultValue = attribute.getBaseValue();
        final double maxHealth = entityHealthService.getMaxHealth(entity);
        final double increment = maxHealth - defaultValue;
        final NamespacedKey key = new NamespacedKey("betterpvp", "health");
        final AttributeModifier modifier = new AttributeModifier(key,
                increment,
                AttributeModifier.Operation.ADD_NUMBER);
        attribute.removeModifier(key);
        attribute.addTransientModifier(modifier);

        // Show health scale for players
        if (entity instanceof Player player) {
            player.setHealthScale(20.0);
        }

        // Update relative health. We don't want people equipping sets and staying half health
        entity.setHealth(Math.max(0.1, Math.min(attribute.getValue(), previousPercentage * attribute.getValue())));
    }
}
