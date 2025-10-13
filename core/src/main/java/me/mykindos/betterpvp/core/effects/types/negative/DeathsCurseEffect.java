package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.combat.cause.EnvironmentalDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

public class DeathsCurseEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Death's Curse";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.WITHER;
    }

    @Override
    public void onTick(LivingEntity livingEntity, Effect effect) {
        final Location location = livingEntity.getLocation();
        Particle.DUST_PILLAR.builder()
                .data(Material.BLACK_TERRACOTTA.createBlockData())
                .location(location)
                .count(10)
                .extra(0.01)
                .offset(livingEntity.getWidth() / 2, 0, livingEntity.getWidth() / 2)
                .allPlayers()
                .spawn();

        final int currentTick = Bukkit.getCurrentTick();
        if (currentTick % 30 == 0) {
            new SoundEffect(Sound.ENTITY_VEX_AMBIENT, (float) Math.random(), (float) Math.random() + 0.3F).play(livingEntity.getLocation());
        }
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        super.onExpire(livingEntity, effect, notify);
        // Only if the effect has expired
        if (effect.hasExpired()) {
            // Set the killer
            final LivingEntity entity = effect.getApplier().get();
            livingEntity.setKiller(entity instanceof Player player ? player : null);

            // Damage them
            final EnvironmentalDamageCause cause = new EnvironmentalDamageCause("deaths_curse",
                    "Death's Curse",
                    EntityDamageEvent.DamageCause.KILL,
                    true,
                    0L,
                    false);

            UtilDamage.doDamage(new DamageEvent(
                    livingEntity,
                    entity,
                    null,
                    cause,
                    Double.MAX_VALUE,
                    "Death's Curse"
            ));
        } else {
            if (notify) {
                UtilMessage.message(livingEntity, "Death's Curse", "You are no longer cursed to death.");
            }
        }
    }

    @Override
    public String getDescription(int level) {
        return "<white>Death's Curse</white> will take the life of the target upon expiring.";
    }

}
