package me.mykindos.betterpvp.core.effects.types.negative;

import lombok.Setter;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * Slows a wielder who has more high-rarity weapons drawn than they are allowed to carry.
 * <p>
 * Deliberately not a {@link SlownessEffect}, which additionally gates movement abilities - several of the
 * weapons this effect counts are themselves movement tools, so reusing slowness would mean the penalty for
 * carrying too many legendaries is that the legendaries stop working.
 */
public class OverloadedEffect extends EffectType {

    private static final NamespacedKey NAMESPACED_KEY = new NamespacedKey("betterpvp", "overloaded");
    private static final double MAX_REDUCTION = 0.95;

    /**
     * Effect types are static singletons constructed before Guice exists, so the configured per-level
     * reduction is pushed in by the listener that owns the config rather than injected here.
     */
    @Setter
    private static double speedReductionPerLevel = 0.10;

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        applyModifier(livingEntity, effect.getAmplifier());
    }

    @Override
    public void onTick(LivingEntity livingEntity, Effect effect) {
        // The level is updated in place as the meter ramps, which never re-fires onReceive
        final AttributeInstance attribute = livingEntity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        final AttributeModifier existing = attribute.getModifier(NAMESPACED_KEY);
        if (existing != null && existing.getAmount() == -getReduction(effect.getAmplifier())) {
            return;
        }

        applyModifier(livingEntity, effect.getAmplifier());
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        final AttributeInstance attribute = livingEntity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(NAMESPACED_KEY);
        }
    }

    private void applyModifier(LivingEntity livingEntity, int level) {
        final AttributeInstance attribute = livingEntity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(NAMESPACED_KEY);
        attribute.addTransientModifier(new AttributeModifier(NAMESPACED_KEY,
                -getReduction(level),
                AttributeModifier.Operation.ADD_SCALAR));
    }

    public double getReduction(int level) {
        return Math.min(MAX_REDUCTION, Math.max(0, level) * speedReductionPerLevel);
    }

    @Override
    public String getName() {
        return "Overloaded";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public boolean mustBeManuallyRemoved() {
        return true;
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> players have too many powerful weapons drawn, moving <stat>"
                + UtilFormat.formatNumber(getReduction(level) * 100, 2) + "%</stat> slower.";
    }

    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white> players have too many powerful weapons drawn, slowing them down.";
    }

    @Override
    public Component getGenericDescriptionComponent() {
        return Translations.component("core.effect.overloaded.generic",
                Component.text(getName(), NamedTextColor.WHITE));
    }

}
