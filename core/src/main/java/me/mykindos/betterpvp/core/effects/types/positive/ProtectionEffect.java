package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.LivingEntity;

/**
 * Provides immunity to negative effects
 */
public class ProtectionEffect extends EffectType {

    @Override
    public String getName() {
        return "Protection";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        super.onExpire(livingEntity, effect, notify);
        if (notify) {
            UtilMessage.message(livingEntity, "Protection", "Your protection has expired.");
        }
    }
}
