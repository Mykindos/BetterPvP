package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class NoJumpEffect extends EffectType {

    @Override
    public String getName() {
        return "No Jump";
    }

    @Override
    public boolean isNegative() {
        return true;
    }


    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        AttributeInstance attribute = livingEntity.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if(attribute != null) {
            attribute.setBaseValue(0);
        }
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect) {
        AttributeInstance attribute = livingEntity.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if(attribute != null) {
            attribute.setBaseValue(attribute.getDefaultValue());
        }
    }


}

