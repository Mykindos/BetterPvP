package me.mykindos.betterpvp.core.interaction.supplier;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.item.config.ConfigEntry;

import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;

/**
 * A functional implementation of {@link ToLongFunction} that computes the delay
 * in milliseconds between item attacks based on the damage event's attack delay
 * or the item's melee attack speed stat.
 * <p>
 * Priority order:
 * <ol>
 *   <li>Item's melee attack speed stat</li>
 *   <li>Damage event attack delay (from {@link InputMeta#DAMAGE_EVENT})</li>
 *   <li>Fallback supplier</li>
 * </ol>
 * The stat comes first because the melee damage delay is a fixed invulnerability window that no longer
 * narrows with attack speed - reading it first would pace every weapon's abilities identically.
 *
 * @see StatTypes#MELEE_ATTACK_SPEED
 */
public class ItemAttackSpeedSupplier implements ToLongFunction<InteractionContext> {

    private final LongSupplier supplier;

    public ItemAttackSpeedSupplier(LongSupplier fallbackMilliSupplier) {
        this.supplier = fallbackMilliSupplier;
    }

    public ItemAttackSpeedSupplier(ConfigEntry<Long> fallbackMilliConfig) {
        this(fallbackMilliConfig::get);
    }

    public ItemAttackSpeedSupplier(long fallbackMilliDelay) {
        this(() -> fallbackMilliDelay);
        validate(fallbackMilliDelay);
    }

    private void validate(long delay) {
        Preconditions.checkArgument(delay >= 0, "Hit delay must be non-negative");
    }

    @Override
    public long applyAsLong(InteractionContext context) {
        if (context != null) {
            // First, try to derive the delay from the held item's attack speed
            final Optional<ItemInstance> itemOpt = context.get(InteractionContext.HELD_ITEM);
            if (itemOpt.isPresent()) {
                final ItemInstance item = itemOpt.get();
                final Optional<StatContainerComponent> component = item.getComponent(StatContainerComponent.class);
                if (component.isPresent()) {
                    final StatContainerComponent container = component.get();
                    final Optional<ItemStat<Double>> statOpt = container.getStat(StatTypes.MELEE_ATTACK_SPEED);
                    if (statOpt.isPresent()) {
                        final double value = statOpt.get().getValue();
                        final double attacksPerSecond = 1000L / (DamageCause.DEFAULT_DELAY / (1 + value));
                        if (attacksPerSecond != 0) {
                            // convert to millisecond delay
                            return (long) (1000 / attacksPerSecond);
                        }
                    }
                }

            }

            final Optional<DamageEvent> damageEventOpt = context.get(InputMeta.DAMAGE_EVENT);
            if (damageEventOpt.isPresent()) {
                final DamageEvent damageEvent = damageEventOpt.get();
                final long attackDelay = damageEvent.getDamageDelay();
                if (attackDelay > 0) {
                    return attackDelay;
                }
            }
        }

        long delay = supplier.getAsLong();
        validate(delay);
        return delay;
    }
}
