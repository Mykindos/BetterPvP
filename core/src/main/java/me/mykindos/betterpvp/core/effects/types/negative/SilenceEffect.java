package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class SilenceEffect extends VanillaEffectType {


    @Override
    public String getName() {
        return "Silence";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.LUCK;
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        super.onReceive(livingEntity, effect);

        livingEntity.getWorld().playSound(livingEntity.getLocation(), Sound.ENTITY_BAT_AMBIENT, 2.0F, 1.0F);
        UtilMessage.simpleMessage(livingEntity, "Silence", "You have been silenced for <alt>%s</alt> seconds.", effect.getRawLength() / 1000d);
    }

    @Override
    public String getDescription(int level) {
        return "<white>Silence</white> prevents the use of skills";
    }
}
