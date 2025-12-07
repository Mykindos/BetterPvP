package me.mykindos.betterpvp.core.item.component.impl.runes.flameguard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.combat.modifiers.impl.GenericModifier;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
public class FlameguardRuneHandler implements Listener {

    private final FlameguardRune flameguardRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public FlameguardRuneHandler(FlameguardRune flameguardRune, ComponentLookupService lookupService) {
        this.flameguardRune = flameguardRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!List.of(EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.CAMPFIRE, EntityDamageEvent.DamageCause.LAVA).contains(event.getBukkitCause())) {
            return; // Not fire / lava
        }

        if (!event.isDamageeLiving()) {
            return;
        }

        final LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
        final EntityEquipment equipment = damagee.getEquipment();
        if (equipment == null) {
            return;
        }

        double mitigation = 0;
        for (ItemStack armorContent : equipment.getArmorContents()) {
            final Optional<RuneContainerComponent> container = componentLookupService.getComponent(armorContent, RuneContainerComponent.class);
            if (container.isEmpty()) {
                continue; // No runes present
            }

            final RuneContainerComponent runeContainer = container.get();
            if (runeContainer.hasRune(flameguardRune)) {
                mitigation += flameguardRune.getMitigation();
            }
        }

        if (mitigation <= 0) {
            return;
        }
        event.addModifier(new GenericModifier("Flameguard",
                ModifierType.RUNE,
                DamageOperator.MULTIPLIER,
                Math.max(0, 1 - mitigation)));
    }
}
