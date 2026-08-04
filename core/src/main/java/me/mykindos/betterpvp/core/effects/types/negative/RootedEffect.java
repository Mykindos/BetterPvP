package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Locks the target to the spot. Rooted entities cannot walk, cannot jump, cannot be moved by
 * velocity, and cannot use movement skills. It carries no visuals of its own and offers no way to
 * break out early, so whatever applies it owns both the presentation and the duration.
 */
public class RootedEffect extends EffectType {

    private static final NamespacedKey NAMESPACED_KEY = new NamespacedKey("betterpvp", "rooted");

    /**
     * Re-sent every tick with no fade, so it holds for exactly as long as the effect does.
     */
    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofMillis(100),
            Duration.ZERO);

    @Override
    public String getName() {
        return "Rooted";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    /**
     * A second application replaces the first, so the shared attribute modifiers are only ever
     * owned by one effect instance and cannot be stripped early by the shorter one expiring.
     */
    @Override
    public boolean canStack() {
        return false;
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        applyModifier(livingEntity, Attribute.MOVEMENT_SPEED,
                new AttributeModifier(NAMESPACED_KEY, -1, AttributeModifier.Operation.ADD_SCALAR));
        applyModifier(livingEntity, Attribute.JUMP_STRENGTH,
                new AttributeModifier(NAMESPACED_KEY, -Integer.MAX_VALUE, AttributeModifier.Operation.ADD_NUMBER));

        if (livingEntity instanceof Player player) {
            UtilPlayer.setWarningEffect(player, 1);
        }
    }

    @Override
    public void onTick(LivingEntity livingEntity, Effect effect) {
        if (!(livingEntity instanceof Player player) || !player.isValid()) {
            return;
        }

        player.showTitle(Title.title(
                Translations.component("core.effect.rooted.title").color(NamedTextColor.RED),
                Component.empty(),
                TITLE_TIMES));
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        removeModifier(livingEntity, Attribute.MOVEMENT_SPEED);
        removeModifier(livingEntity, Attribute.JUMP_STRENGTH);

        if (livingEntity instanceof Player player) {
            UtilPlayer.clearWarningEffect(player);
        }
    }

    private void applyModifier(LivingEntity livingEntity, Attribute attribute, AttributeModifier modifier) {
        final AttributeInstance instance = livingEntity.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(NAMESPACED_KEY);
        instance.addTransientModifier(modifier);
    }

    private void removeModifier(LivingEntity livingEntity, Attribute attribute) {
        final AttributeInstance instance = livingEntity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(NAMESPACED_KEY);
        }
    }

    @Override
    public String getDescription(int level) {
        return getGenericDescription();
    }

    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white> players are held in place, unable to move, jump, or use movement skills.";
    }

    @Override
    public Component getGenericDescriptionComponent() {
        return Translations.component("core.effect.rooted.generic",
                Component.text(getName(), NamedTextColor.WHITE));
    }
}
