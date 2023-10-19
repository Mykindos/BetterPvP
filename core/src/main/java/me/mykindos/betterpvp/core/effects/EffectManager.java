package me.mykindos.betterpvp.core.effects;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

@Singleton
public class EffectManager extends Manager<List<Effect>> {

    public void addEffect(LivingEntity target, EffectType type, long length) {
        addEffect(target, type, 1, length);
    }

    public void addEffect(LivingEntity target, EffectType type, int level, long length) {
       UtilServer.callEvent(new EffectReceiveEvent(target, new Effect(target.getUniqueId().toString(), type, level, length)));

    }

    public Optional<Effect> getEffect(LivingEntity target, EffectType type) {

        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            return effects.stream().filter(effect -> effect.getUuid().equalsIgnoreCase(target.getUniqueId().toString())
                    && effect.getEffectType() == type).findFirst();
        } else {
            return Optional.empty();
        }


    }

    public boolean hasEffect(LivingEntity target, EffectType type) {
        return getEffect(target, type).isPresent();
    }

    public void removeEffect(LivingEntity target, EffectType type) {
        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            effects.removeIf(effect -> {
                if(effect.getUuid().equals(target.getUniqueId().toString()) && effect.getEffectType() == type) {
                    UtilServer.callEvent(new EffectExpireEvent(target, effect));
                    return true;
                }
                return false;
            });
        });

    }


    public void removeAllEffects(LivingEntity target){
        objects.remove(target.getUniqueId().toString());
    }

}
