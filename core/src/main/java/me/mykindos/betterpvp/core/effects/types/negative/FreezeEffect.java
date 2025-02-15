package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import org.bukkit.entity.LivingEntity;

public class FreezeEffect extends EffectType {

    @Override
    public String getName() {
        return "Freeze";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public boolean canStack() {
        return false;
    }

    @Override
    public void onTick(LivingEntity entity, Effect effect) {
        entity.setFreezeTicks(180);
    }

    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        livingEntity.setFreezeTicks(0);
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> hinders movement and field of vision";
    }
}
