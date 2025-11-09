package me.mykindos.betterpvp.core.item.component.impl.runes.greed;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.framework.economy.CoinDropEvent;
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
public class GreedRuneHandler implements Listener {

    private final GreedRune greedRune;
    private final DamageLogManager damageLogManager;
    private final ComponentLookupService componentLookupService;

    @Inject
    public GreedRuneHandler(GreedRune greedRune, DamageLogManager damageLogManager, ComponentLookupService lookupService) {
        this.greedRune = greedRune;
        this.damageLogManager = damageLogManager;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCoinDrop(CoinDropEvent event) {
        final DamageLog lastDamage = damageLogManager.getLastDamage(event.getPlayer());
        if (lastDamage == null) {
            return; // Wasn't killed by a player
        }

        if (!(lastDamage.getDamager() instanceof LivingEntity damager)) {
            return; // Wasn't killed by a player
        }

        if (!lastDamage.getDamageCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            return; // Wasn't a melee hit
        }

        final EntityEquipment equipment = damager.getEquipment();
        if (equipment == null) {
            return;
        }

        final ItemStack item = damager.getEquipment().getItemInMainHand();
        final Optional<RuneContainerComponent> container = componentLookupService.getComponent(item, RuneContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        // Check if the scorching rune is present in the container
        if (!container.get().hasRune(greedRune)) {
            return; // Rune not present
        }

        // Apply the percentage bonus for each rune found
        event.setPercentageBonus(event.getPercentageBonus() + greedRune.getPercentageBonus());
    }
}
