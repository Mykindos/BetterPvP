package me.mykindos.betterpvp.core.effects;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Singleton
public class EffectManager extends Manager<List<Effect>> {

    public void addEffect(LivingEntity target, EffectType type, long length) {
        addEffect(target, type, type.defaultAmplifier(), length);
    }

    public void addEffect(LivingEntity target, EffectType type, int level, long length) {
        addEffect(target, null, type, level, length);
    }

    public void addEffect(LivingEntity target, EffectType type, String name, int level, long length) {
        addEffect(target, null, type, name, level, length);
    }

    public void addEffect(LivingEntity target, EffectType type, String name, int level, long length, boolean overwrite) {
        addEffect(target, null, type, name, level, length, overwrite);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, long length) {
        addEffect(target, applier, type, type.defaultAmplifier(), length);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, int level, long length) {
        addEffect(target, applier, type, "", level, length);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length) {
        addEffect(target, applier, type, name, level, length, false);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length, boolean overwrite) {
        addEffect(target, applier, type, name, level, length, overwrite, false);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length, boolean overwrite, boolean permanent) {
        Effect effect = new Effect(target.getUniqueId().toString(), applier, type, name, level, length, permanent);
        EffectReceiveEvent event = UtilServer.callEvent(new EffectReceiveEvent(target, effect));

        if (!event.isCancelled()) {

            if (!type.canStack()) {
                if (hasEffect(target, effect.getEffectType())) {
                    removeEffect(target, effect.getEffectType());
                }
            }

            if (overwrite) {
                Effect overwriteEffect = getEffect(target, type, name).orElse(null);
                if (overwriteEffect != null) {
                    overwriteEffect.setAmplifier(level);
                    overwriteEffect.setLength(length);

                    if(effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                        vanillaEffectType.checkActive(target, effect);
                    }

                    return;
                }
            }

            Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId()).or(() -> {
                List<Effect> effects = new ArrayList<>();
                addObject(target.getUniqueId().toString(), effects);
                return Optional.of(effects);
            });

            if (effectsOptional.isPresent()) {
                List<Effect> effects = effectsOptional.get();
                effects.add(effect);
                effect.getEffectType().onReceive(target, effect);
            }
        }
    }


    public Optional<Effect> getEffect(LivingEntity target, EffectType type) {

        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            return effects.stream().filter(effect -> effect.getUuid().equalsIgnoreCase(target.getUniqueId().toString())
                            && effect.getEffectType() == type)
                    .max(Comparator.comparingInt(Effect::getAmplifier));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Effect> getEffect(LivingEntity target, EffectType type, String name) {

        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            return effects.stream().filter(effect -> effect.getUuid().equalsIgnoreCase(target.getUniqueId().toString())
                            && effect.getEffectType() == type && effect.getName().equalsIgnoreCase(name))
                    .max(Comparator.comparingInt(Effect::getAmplifier));
        } else {
            return Optional.empty();
        }


    }

    public boolean hasEffect(LivingEntity target, EffectType type) {
        return getEffect(target, type).isPresent();
    }

    public boolean hasEffect(LivingEntity target, EffectType type, String name) {
        return getEffect(target, type, name).isPresent();
    }

    public void removeEffect(LivingEntity target, EffectType type) {
        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            effects.removeIf(effect -> {
                if (effect.getEffectType() == type) {

                    if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                        vanillaEffectType.onExpire(target, effect);
                    }

                    UtilServer.callEvent(new EffectExpireEvent(target, effect));
                    return true;
                }
                return false;
            });
        });

    }

    public void removeEffect(LivingEntity target, EffectType type, String name) {
        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            effects.removeIf(effect -> {
                if (effect.getEffectType() == type && effect.getName().equalsIgnoreCase(name)) {

                    UtilServer.callEvent(new EffectExpireEvent(target, effect));
                    return true;
                }
                return false;
            });
        });

    }


    public void removeAllEffects(LivingEntity target) {
        var effects = objects.remove(target.getUniqueId().toString());
        if(effects != null) {
            effects.forEach(effect -> UtilServer.callEvent(new EffectExpireEvent(target, effect)));
        }
    }

    public void removeNegativeEffects(LivingEntity target) {
        for (PotionEffect pot : target.getActivePotionEffects()) {
            if (UtilEffect.isNegativePotionEffect(pot)) {
                target.removePotionEffect(pot.getType());
            }
        }

        for (EffectType effect : EffectTypes.getEffectTypes()) {
            if (!effect.isNegative()) continue;
            removeEffect(target, effect);
        }

        target.setFireTicks(0);
    }

}
