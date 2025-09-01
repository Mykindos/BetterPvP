package me.mykindos.betterpvp.core.combat.health;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
    void onArmor(EntityEquipmentChangedEvent event) {
        final LivingEntity entity = event.getEntity();
        if (entity instanceof ArmorStand) {
            return;
        }

        final AttributeInstance attribute = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH));
        final double previousPercentage = entity.getHealth() / attribute.getValue();

        // Update max health
        final double defaultValue = attribute.getDefaultValue();
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
        entity.setHealth(Math.max(1.0, previousPercentage * attribute.getValue()));
    }
}
