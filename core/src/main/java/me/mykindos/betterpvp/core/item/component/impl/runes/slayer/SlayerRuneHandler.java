package me.mykindos.betterpvp.core.item.component.impl.runes.slayer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.combat.modifiers.impl.GenericModifier;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class SlayerRuneHandler implements Listener {

    private final SlayerRune slayerRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public SlayerRuneHandler(SlayerRune slayerRune, ComponentLookupService lookupService) {
        this.slayerRune = slayerRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager) || !event.isDamageeLiving()) {
            return; // Only handle player damage events
        }

        if (event.getDamagee() instanceof Player) {
            return; // Only do extra damage to mobs
        }

        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            return; // Only handle melee attacks
        }

        final EntityEquipment equipment = damager.getEquipment();
        if (equipment == null) {
            return;
        }

        final ItemStack item = equipment.getItemInMainHand();
        final Optional<RuneContainerComponent> container = componentLookupService.getComponent(item, RuneContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        // Check if the slayer rune is present in the container
        if (!container.get().hasRune(slayerRune)) {
            return; // Slayer rune not present
        }

        event.addModifier(new GenericModifier(
                slayerRune.getName(),
                ModifierType.RUNE,
                1.0,
                Math.max(0, slayerRune.getDamage())
        ));
    }
}
