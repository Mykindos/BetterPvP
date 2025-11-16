package me.mykindos.betterpvp.core.item.component.impl.runes.moonseer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class MoonseerRuneHandler implements Listener {

    private final EffectManager effectManager;
    private final MoonseerRune moonseerRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public MoonseerRuneHandler(EffectManager effectManager, MoonseerRune moonseerRune, ComponentLookupService lookupService) {
        this.effectManager = effectManager;
        this.moonseerRune = moonseerRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquip(EntityEquipmentChangedEvent event) {
        final LivingEntity entity = event.getEntity();
        final EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return;
        }

        for (ItemStack armorContent : equipment.getArmorContents()) {
            final Optional<RuneContainerComponent> container = componentLookupService.getComponent(armorContent, RuneContainerComponent.class);
            if (container.isEmpty()) {
                continue; // No runes present
            }

            final RuneContainerComponent runeContainer = container.get();
            // has moonseer
            if (runeContainer.hasRune(moonseerRune)) {
                this.effectManager.addEffect(entity, null, EffectTypes.NIGHT_VISION, "Moonseer", 1, Long.MAX_VALUE, true, true, false, null);
                return;
            }
        }

        this.effectManager.removeEffect(entity, EffectTypes.NIGHT_VISION, "Moonseer");
    }
}
