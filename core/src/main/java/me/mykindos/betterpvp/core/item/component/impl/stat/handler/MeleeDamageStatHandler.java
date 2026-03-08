package me.mykindos.betterpvp.core.item.component.impl.stat.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@Singleton
@BPvPListener
public class MeleeDamageStatHandler implements Listener {

    private final ItemFactory itemFactory;

    @Inject
    private MeleeDamageStatHandler(ItemFactory itemFactory, CoreListenerLoader listenerLoader) {
        this.itemFactory = itemFactory;
        listenerLoader.register(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            return; // Only melee damage causes are handled
        }

        if (!(event.getDamager() instanceof LivingEntity livingEntity)) {
            return; // Only living entities can deal melee damage
        }

        final EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) {
            return; // No equipment, no item
        }

        final ItemStack item = equipment.getItemInMainHand();
        itemFactory.fromItemStack(item).ifPresent(itemInstance -> {
            Optional<StatContainerComponent> statContainerOpt = itemInstance.getComponent(StatContainerComponent.class);
            if (statContainerOpt.isEmpty()) {
                return; // No stat container, no stats
            }

            final Optional<ItemStat<Double>> statOpt = statContainerOpt.get().getStat(StatTypes.MELEE_DAMAGE);
            if (statOpt.isEmpty()) {
                return; // No damage stat, no damage
            }

            final ItemStat<Double> stat = statOpt.get();
            event.setDamage(stat.getValue());
        });
    }

}
