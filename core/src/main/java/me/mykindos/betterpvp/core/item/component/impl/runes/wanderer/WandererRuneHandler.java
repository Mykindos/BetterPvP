package me.mykindos.betterpvp.core.item.component.impl.runes.wanderer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

@BPvPListener
@Singleton
public class WandererRuneHandler implements Listener {

    private final Object DUMMY_OBJECT = new Object();
    private final DamageLogManager damageLogManager;
    private final WandererRune wandererRune;
    private final WeakHashMap<LivingEntity, Object> tracked = new WeakHashMap<>();
    private final ComponentLookupService componentLookupService;

    @Inject
    public WandererRuneHandler(DamageLogManager damageLogManager, WandererRune wandererRune, ComponentLookupService lookupService) {
        this.damageLogManager = damageLogManager;
        this.wandererRune = wandererRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquip(EntityEquipmentChangedEvent event) {
        final LivingEntity entity = event.getEntity();
        final EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return;
        }

        final AttributeInstance attribute = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(WandererRune.KEY);
        tracked.remove(entity);
        for (ItemStack armorContent : equipment.getArmorContents()) {
            final Optional<RuneContainerComponent> container = componentLookupService.getComponent(armorContent, RuneContainerComponent.class);
            if (container.isEmpty()) {
                continue; // No runes present
            }

            final RuneContainerComponent runeContainer = container.get();
            if (runeContainer.hasRune(wandererRune)) {
                tracked.put(entity, DUMMY_OBJECT);
                return;
            }
        }
    }

    @UpdateEvent
    public void onTick() {
        final Iterator<LivingEntity> iterator = tracked.keySet().iterator();
        while (iterator.hasNext()) {
            LivingEntity entity = iterator.next();
            final AttributeInstance attribute = Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED));
            if (entity.isDead() || !entity.isValid() || (entity instanceof Player player && !player.isOnline())) {
                attribute.removeModifier(WandererRune.KEY);
                iterator.remove();
                continue;
            }

            // Check if they're in combat
            final DamageLog lastDamager = damageLogManager.getLastDamager(entity);
            if (lastDamager == null || lastDamager.getDamager() == null) {
                // They haven't taken damage by an enemy
                // Give them the effect
                final AttributeModifier modifier = new AttributeModifier(WandererRune.KEY,
                        wandererRune.getSpeed(),
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1);
                if (attribute.getModifier(WandererRune.KEY) == null) {
                    attribute.addTransientModifier(modifier);
                }
            } else {
                // They took damage
                // Take it away
                attribute.removeModifier(WandererRune.KEY);
            }
        }
    }
}
