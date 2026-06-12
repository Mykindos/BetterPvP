package me.mykindos.betterpvp.core.effects;

import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

@Data
public abstract class EffectType {

    public abstract String getName();
    public abstract boolean isNegative();

    /**
     * If true, players can receive multiple instances of this effect
     * If false and a player receives this effect, it will replace the current effect
     * @return True if the effect can stack
     */
    public boolean canStack() {
        return true;
    }

    public int defaultAmplifier() {
        return 1;
    }

    public void onReceive(LivingEntity livingEntity, Effect effect) {

    }

    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {

    }

    public void onTick(LivingEntity livingEntity, Effect effect) {

    }

    public String getDescription(int level) {
        return "";
    }

    public String getGenericDescription() {
        return "";
    }

    /**
     * The player-facing, translatable generic description of this effect type. Defaults to an empty
     * component; effect types with a generic description should override this with a translatable
     * component (e.g. {@code core.effect.bleed.generic}).
     *
     * @return the translatable generic-description component
     */
    public Component getGenericDescriptionComponent() {
        return Component.empty();
    }

    public boolean mustBeManuallyRemoved() {
        return false;
    }

    public boolean isSpecial() {
        return false;
    }

}
