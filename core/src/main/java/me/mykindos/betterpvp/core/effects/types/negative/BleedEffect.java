package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BleedEffect extends VanillaEffectType {

    private final Map<UUID, Long> lastBleedTimes = new HashMap<>();

    @Override
    public String getName() {
        return "Bleed";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.BAD_OMEN;
    }

    @Override
    public void onTick(LivingEntity livingEntity, Effect effect) {
        super.onTick(livingEntity, effect);

        if (!livingEntity.hasPotionEffect(PotionEffectType.BAD_OMEN)) return;

        long currentTime = System.currentTimeMillis();
        long lastBleedTime = lastBleedTimes.computeIfAbsent(livingEntity.getUniqueId(), uuid -> currentTime - 950); // Delay 1 tick

        if (UtilTime.elapsed(lastBleedTime, 1000)) {
            // Apply damage to any LivingEntity (including players)

            var cde = new CustomDamageEvent(livingEntity, effect.getApplier(), null, EntityDamageEvent.DamageCause.CUSTOM, 1.5, false, "Bleed");
            cde.setIgnoreArmour(true);
            UtilDamage.doCustomDamage(cde);

            livingEntity.getWorld().playSound(livingEntity.getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 2f);
            livingEntity.getWorld().playEffect(livingEntity.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.REDSTONE_BLOCK);

            lastBleedTimes.put(livingEntity.getUniqueId(), currentTime);
        }
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect) {
        super.onExpire(livingEntity, effect);
        lastBleedTimes.remove(livingEntity.getUniqueId());
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> deals <val>1.5</val> damage per second";
    }


    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white> deals <green>1.5</green> damage per second";
    }
}
