package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class AntiHealEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Anti Heal";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.INFESTED;
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect) {
        super.onExpire(livingEntity, effect);
        UtilMessage.message(livingEntity, "Anti Heal", "You can now regenerate health!");
    }

    @Override
    public String getDescription(int level) {
        return "<white>Anti Heal " + UtilFormat.getRomanNumeral(level) + " <reset>stops you from being able to regenerate health";
    }

}
