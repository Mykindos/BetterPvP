package me.mykindos.betterpvp.core.effects;

import lombok.Data;
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

    public void onExpire(LivingEntity livingEntity, Effect effect) {

    }

    public void onTick(LivingEntity livingEntity, Effect effect) {

    }

    public String getDescription(int level) {
        return "";
    }

    public String getGenericDescription() {
        return "";
    }

    public boolean mustBeManuallyRemoved() {
        return false;
    }

}
