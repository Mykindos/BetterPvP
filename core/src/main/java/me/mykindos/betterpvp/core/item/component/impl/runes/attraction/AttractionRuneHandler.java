package me.mykindos.betterpvp.core.item.component.impl.runes.attraction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@BPvPListener
@Singleton
public class AttractionRuneHandler implements Listener {

    private final WeakHashMap<LivingEntity, Double> pullSpeedMap = new WeakHashMap<>();
    private final AttractionRune attractionRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public AttractionRuneHandler(AttractionRune attractionRune, ComponentLookupService lookupService) {
        this.attractionRune = attractionRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquip(EntityEquipmentChangedEvent event) {
        final LivingEntity entity = event.getEntity();
        final EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return;
        }

        double speed = 0;
        for (ItemStack armorContent : equipment.getArmorContents()) {
            final Optional<RuneContainerComponent> container = componentLookupService.getComponent(armorContent, RuneContainerComponent.class);
            if (container.isEmpty()) {
                continue; // No runes present
            }

            final RuneContainerComponent runeContainer = container.get();
            if (runeContainer.hasRune(attractionRune)) {
                speed += attractionRune.getSpeed();
            }
        }

        if (speed <= 0) {
            this.pullSpeedMap.remove(entity);
        } else {
            this.pullSpeedMap.put(entity, speed);
        }
    }

    @UpdateEvent
    public void onTick() {
        // We make an item vector map and act upon it so:
        // 1. Wearers don't have a pull priority, meaning the last player to pull in the iterator will be the prioritized
        // 2. The effective item velocity is calculated and used, rather than the last wearer's pull
        // To solve this, we get the initial velocity of the item (if it's falling down, etc), store it, then add a factor
        // of change (blocksPerSecond / 20) until it reaches blocksPerSecond (3D vector).
        // This will cause people with the same pull velocity to have equal effect on the item, thus making the item
        // stationary until one of the players stops pulling it.
        final Map<Item, Vector> itemVectorMap = new HashMap<>();
        final Iterator<Map.Entry<LivingEntity, Double>> iterator = pullSpeedMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<LivingEntity, Double> entry = iterator.next();
            final LivingEntity entity = entry.getKey();
            final double speed = entry.getValue();
            if (entity.isDead() || !entity.isValid() || (entity instanceof Player player && !player.isOnline())) {
                iterator.remove();
                continue;
            }

            final Collection<Item> nearby = entity.getWorld().getNearbyEntitiesByType(Item.class,
                    entity.getLocation(),
                    attractionRune.getRange());

            for (Item item : nearby) {
                if (item.getTicksLived() < 15) {
                    continue; // Don't attract items that were just dropped to prevent immediate pull
                }

                // Populate so we can act upon it
                final Vector vector = itemVectorMap.computeIfAbsent(item, i -> i.getVelocity().clone());

                // Calculate the direction
                final Vector destination = entity.getLocation().add(0, entity.getEyeHeight() / 2, 0).toVector();
                final Vector directionToPlayer = destination.subtract(item.getLocation().toVector());
                final Vector perSecond = directionToPlayer.clone().normalize().multiply(speed);
                final Vector perTick = perSecond.clone().multiply(1 / 20d);// Convert to per tick

                // Add the direction to the vector but clamp it (both negative and positive) to the directionToPlayer
                final Vector difference = perTick.clone().subtract(vector);
                if (difference.lengthSquared() < perTick.lengthSquared()) {
                    vector.add(difference);
                } else {
                    vector.add(perTick);
                }
            }
        }

        // Apply the vectors to the items
        for (Map.Entry<Item, Vector> entry : itemVectorMap.entrySet()) {
            if (!entry.getValue().toVector3d().isFinite()) {
                continue;
            }
            entry.getKey().setVelocity(entry.getValue());
        }
    }
}
