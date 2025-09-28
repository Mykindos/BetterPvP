package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class RegenerationEffect extends VanillaEffectType {

    public final Map<LivingEntity, Long> lastHeal = new WeakHashMap<>();
    private final EntityHealthService healthService;

    public RegenerationEffect() {
        this.healthService = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(EntityHealthService.class);
    }

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
        if (lastHealTime - System.currentTimeMillis() <= 0) {
            // Vanilla regen heals 1.0 (out of 20.0) health every time it is ticked.
            // That means that it heals 1/20 (5%) of a player's total health
            // Since health is scaled in BetterPvP (armor can increase health), we will port this 5%
            // behavior over instead of healing 1.0 over every time the effect is ticked (which is
            // effectively no health for higher health individuals).
            //
            // This makes regeneration really good and we should opt to use other conventional
            // healing methods like directly giving a player's health.
            final double maxHealth = healthService.getMaxHealth(livingEntity);
            UtilEntity.health(livingEntity, maxHealth * 0.05);
            lastHeal.put(livingEntity, (long) (System.currentTimeMillis() + Math.max(1, Math.floor((50 / Math.pow(2, effect.getAmplifier() -1)))) * 50));
        }
    }
}

