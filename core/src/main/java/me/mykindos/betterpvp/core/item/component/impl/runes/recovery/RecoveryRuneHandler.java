package me.mykindos.betterpvp.core.item.component.impl.runes.recovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class RecoveryRuneHandler implements Listener {

    private final RecoveryRune recoveryRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public RecoveryRuneHandler(RecoveryRune recoveryRune, ComponentLookupService lookupService) {
        this.recoveryRune = recoveryRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        final EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) {
            return;
        }

        double flatIncrement = 0;
        for (ItemStack armorContent : equipment.getArmorContents()) {
            final Optional<RuneContainerComponent> container = componentLookupService.getComponent(armorContent, RuneContainerComponent.class);
            if (container.isEmpty()) {
                continue; // No runes present
            }

            final RuneContainerComponent runeContainer = container.get();
            if (runeContainer.hasRune(recoveryRune)) {
                flatIncrement += recoveryRune.getIncrement();
            }
        }

        if (flatIncrement <= 0) {
            return;
        }
        event.setAmount(event.getAmount() + flatIncrement);
    }
}
