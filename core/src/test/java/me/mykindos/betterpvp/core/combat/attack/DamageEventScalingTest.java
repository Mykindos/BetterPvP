package me.mykindos.betterpvp.core.combat.attack;

import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.combat.modifiers.impl.GenericModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Damage event attack strength scaling")
class DamageEventScalingTest {

    private static final double DELTA = 1e-9;

    private DamageEvent event(double baseDamage) {
        return new DamageEvent(Mockito.mock(Entity.class),
                null,
                null,
                Mockito.mock(DamageSource.class),
                new TestDamageCause(),
                baseDamage);
    }

    private DamageEvent mixedEvent(double baseDamage) {
        final DamageEvent event = event(baseDamage);
        event.addModifier(new GenericModifier("Weapon Stat", DamageOperator.FLAT, 3.0).withType(ModifierType.STAT));
        event.addModifier(new GenericModifier("Strength", DamageOperator.FLAT, 1.5).withType(ModifierType.EFFECT));
        event.addModifier(new GenericModifier("Slayer Rune", DamageOperator.FLAT, 2.0).withType(ModifierType.RUNE));
        event.addModifier(new GenericModifier("Backstab", DamageOperator.FLAT, 4.0).withType(ModifierType.ABILITY));
        return event;
    }

    @Test
    @DisplayName("A full strength swing is identical to the unscaled result")
    void fullStrengthIsUnchanged() {
        final DamageEvent event = mixedEvent(6.0);

        // 6 base + 3 stat + 1.5 strength + 2 rune + 4 ability
        assertEquals(16.5, event.getModifiedDamage(), DELTA);
    }

    @Test
    @DisplayName("Multipliers and reductions survive the bucket split unchanged at full strength")
    void multipliersAndReductionsAreUnchangedAtFullStrength() {
        final DamageEvent event = mixedEvent(6.0);
        event.addModifier(new GenericModifier("Fortify", DamageOperator.MULTIPLIER, 1.5).withType(ModifierType.ABILITY));
        event.addModifier(new GenericModifier("Armour", DamageOperator.MULTIPLIER, 0.5).withType(ModifierType.ARMOR));

        // ((6 * 1.5) + 3 + 1.5 + 2 + 4) * 0.5
        assertEquals(9.75, event.getModifiedDamage(), DELTA);
    }

    @Test
    @DisplayName("A half charged swing shrinks weapon and rune damage but not ability damage")
    void halfChargeScalesPerBucket() {
        final DamageEvent event = mixedEvent(6.0);
        event.setAttackStrengthScale(0.5);

        // base (6 + 3 + 1.5) * 0.4, rune 2 * 0.5, ability 4 unscaled
        assertEquals(4.2 + 1.0 + 4.0, event.getModifiedDamage(), DELTA);
    }

    @Test
    @DisplayName("A critical hit multiplies base damage only")
    void criticalMultipliesBaseOnly() {
        final DamageEvent event = mixedEvent(6.0);
        event.setCritical(true);

        // base (6 + 3 + 1.5) * 1.5, rune 2, ability 4
        assertEquals(15.75 + 2.0 + 4.0, event.getModifiedDamage(), DELTA);
    }

    @Test
    @DisplayName("Reductions apply to the scaled total and are never scaled themselves")
    void reductionsAreNotScaled() {
        final DamageEvent event = event(10.0);
        event.addModifier(new GenericModifier("Armour", DamageOperator.MULTIPLIER, 0.5).withType(ModifierType.ARMOR));
        event.setAttackStrengthScale(0.0);

        // 10 * 0.2 = 2, then halved
        assertEquals(1.0, event.getModifiedDamage(), DELTA);
    }

    @Test
    @DisplayName("Attack strength is clamped to the vanilla range")
    void attackStrengthIsClamped() {
        final DamageEvent event = event(10.0);

        event.setAttackStrengthScale(2.5);
        assertEquals(1.0, event.getAttackStrengthScale(), DELTA);

        event.setAttackStrengthScale(-1.0);
        assertEquals(0.0, event.getAttackStrengthScale(), DELTA);
    }

    private static final class TestDamageCause implements DamageCause {

        @Override
        public String getName() {
            return "TEST";
        }

        @Override
        public String getDisplayName() {
            return "Test";
        }

        @Override
        public boolean isTrueDamage() {
            return false;
        }

        @Override
        public long getDefaultDelay() {
            return DEFAULT_DELAY;
        }

        @Override
        public boolean allowsKnockback() {
            return true;
        }

        @Override
        public Collection<DamageCauseCategory> getCategories() {
            return List.of(DamageCauseCategory.MELEE);
        }

        @Override
        public EntityDamageEvent.DamageCause getBukkitCause() {
            return EntityDamageEvent.DamageCause.ENTITY_ATTACK;
        }
    }
}
