package me.mykindos.betterpvp.core.item.component.impl.runes.ferocity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class FerocityRuneHandler implements Listener {

    private final FerocityRune ferocityRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public FerocityRuneHandler(FerocityRune ferocityRune, ComponentLookupService lookupService) {
        this.ferocityRune = ferocityRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(DamageEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager)) {
            return; // Wasn't damaged by an entity
        }

        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            return; // Wasn't a melee hit
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

        // Check if the rune is present in the container
        if (!container.get().hasRune(ferocityRune)) {
            return; // Rune not present
        }

        // Check for chances
        if (Math.random() <= ferocityRune.getChance()) {
            final double delayReduction = ferocityRune.getDelayReduction();
            event.setDamageDelay((long) Math.max(0, event.getDamageDelay() * (1 - delayReduction)));
            new SoundEffect(Sound.ENTITY_RAVAGER_ROAR, 2f, 1f).play(damager.getLocation());
            new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0f, 1f).play(damager.getLocation());
        }
    }
}
