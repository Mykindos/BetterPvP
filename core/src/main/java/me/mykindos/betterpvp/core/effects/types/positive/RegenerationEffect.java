package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.WeakHashMap;

public class RegenerationEffect extends VanillaEffectType {

    private final WeakHashMap<LivingEntity, Long> lastHeal = new WeakHashMap<>();

    @Override
    public String getName() {
        return "Regeneration";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.REGENERATION;
    }

    @Override
    public String getDescription(int level) {
        //https://minecraft.wiki/w/Regeneration
        int ticks = (int) Math.max(Math.floor((50d/((level - 1)^2))), 1);
        double seconds = ticks / 20d;
        return "<white>" + getName() + " " + UtilFormat.getRomanNumeral(level) + "</white> increases health by an additional <stat>5</stat>% every <val>" + UtilFormat.formatNumber(seconds, 3) + "</val> seconds";
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.sendPacketPotionEffect(livingEntity, PotionEffectType.REGENERATION, effect.getAmplifier() - 1, effect.getVanillaDuration(), false, effect.isShowParticles(), true);
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        UtilEffect.sendPacketPotionEffectRemove(livingEntity, PotionEffectType.REGENERATION);
    }

    @Override
    public void checkActive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.sendPacketPotionEffect(livingEntity, PotionEffectType.REGENERATION, effect.getAmplifier() - 1, effect.getRemainingVanillaDuration(), false, effect.isShowParticles(), true);
    }

    @Override
    public void onTick(LivingEntity livingEntity, Effect effect) {
        super.onTick(livingEntity, effect);

        Long lastHealTime = lastHeal.computeIfAbsent(livingEntity, k -> 0L);
        if(lastHealTime - System.currentTimeMillis() <= 0) {
            UtilEntity.health(livingEntity, 1.0f);
            lastHeal.put(livingEntity, (long) (System.currentTimeMillis() + Math.max(1, Math.floor((50 / Math.pow(2, effect.getAmplifier() -1)))) * 50));
        }
    }
}

