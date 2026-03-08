package me.mykindos.betterpvp.core.item.component.impl.runes.momentum;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
public class MomentumRuneHandler implements Listener {

    private final MomentumRune momentumRune;
    private final ComponentLookupService componentLookupService;
    private final Multimap<LivingEntity, AttackEntry> attackSpeedMap = ArrayListMultimap.create();

    @Inject
    public MomentumRuneHandler(MomentumRune momentumRune, ComponentLookupService lookupService) {
        this.momentumRune = momentumRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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

        // Check if the Rune is present in the container
        if (!container.get().hasRune(momentumRune)) {
            return; // Rune not present
        }

        // See if we can find an existing one and apply the scalar
        final LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
        for (AttackEntry entry : attackSpeedMap.get(damager)) {
            if (entry != null && entry.victim == damagee) {
                // Apply the scalar
                event.setDamageDelay((long) (event.getDamageDelay() * (1 - entry.scalar)));

                // Refresh timestamp
                entry.refreshTimestamp();
                // Add multiplier
                entry.scalar = Math.min(1, entry.scalar + momentumRune.getScalar());
                return;
            }
        }

        // Or we put in a new one
        final AttackEntry entry = new AttackEntry(damagee);
        entry.scalar = momentumRune.getScalar();
        attackSpeedMap.put(damager, entry);
    }

    @UpdateEvent
    public void tick() {
        final Iterator<Map.Entry<LivingEntity, AttackEntry>> iterator = attackSpeedMap.entries().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<LivingEntity, AttackEntry> next = iterator.next();
            final LivingEntity damager = next.getKey();
            if (!damager.isValid() || (damager instanceof Player player && !player.isOnline())) {
                iterator.remove();
                continue;
            }

            final AttackEntry entry = next.getValue();
            if (!entry.isValid() || entry.isExpired()) {
                iterator.remove();
            }
        }
    }

    @RequiredArgsConstructor
    private class AttackEntry {
        private final LivingEntity victim;
        private long timestamp = System.currentTimeMillis();
        private double scalar;

        public boolean isValid() {
            return victim.isValid() && (!(victim instanceof Player playerVictim) || playerVictim.isOnline());
        }

        public boolean isExpired() {
            return UtilTime.elapsed(timestamp, (long) (momentumRune.getExpirySeconds() * 1000L));
        }

        public void refreshTimestamp() {
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            AttackEntry that = (AttackEntry) o;
            return victim.getUniqueId().equals(that.victim.getUniqueId());
        }

        @Override
        public int hashCode() {
            return victim.getUniqueId().hashCode();
        }
    }
}
