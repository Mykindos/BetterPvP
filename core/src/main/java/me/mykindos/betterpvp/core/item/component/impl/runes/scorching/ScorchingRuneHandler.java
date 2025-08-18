package me.mykindos.betterpvp.core.item.component.impl.runes.scorching;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class ScorchingRuneHandler implements Listener {

    private final ScorchingRune scorchingRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public ScorchingRuneHandler(ScorchingRune scorchingRune, ComponentLookupService lookupService) {
        this.scorchingRune = scorchingRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(DamageEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getDamagee() instanceof LivingEntity livingEntity)) {
            return; // Only handle player damage events
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return; // Only handle melee attacks
        }

        final ItemStack item = player.getEquipment().getItemInMainHand();
        final Optional<RuneContainerComponent> container = componentLookupService.getComponent(item, RuneContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        // Check if the scorching rune is present in the container
        if (!container.get().hasRune(scorchingRune)) {
            return; // Scorching rune not present
        }

        // Apply scorching effect
        final double chance = scorchingRune.getChance();
        if (Math.random() < chance) {
            final double seconds = scorchingRune.getDuration();
            final int ticks = (int) (seconds * 20); // Convert seconds to ticks
            livingEntity.setFireTicks(Math.max(livingEntity.getFireTicks(), ticks)); // Apply fire effect, ensuring it doesn't overwrite existing fire ticks
        }
    }
}
