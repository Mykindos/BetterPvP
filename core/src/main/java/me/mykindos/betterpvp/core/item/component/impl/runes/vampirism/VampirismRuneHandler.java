package me.mykindos.betterpvp.core.item.component.impl.runes.vampirism;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class VampirismRuneHandler implements Listener {

    private final VampirismRune vampirismRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public VampirismRuneHandler(VampirismRune vampirismRune, ComponentLookupService lookupService) {
        this.vampirismRune = vampirismRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager) || !event.isDamageeLiving()) {
            return; // Only handle player damage events
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

        // Check if the vampirism rune is present in the container
        if (!container.get().hasRune(vampirismRune)) {
            return; // Vampirism rune not present
        }

        UtilPlayer.health(damager, vampirismRune.getHealing());
    }
}
