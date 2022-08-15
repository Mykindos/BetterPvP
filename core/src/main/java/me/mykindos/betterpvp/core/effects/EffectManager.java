package me.mykindos.betterpvp.core.effects;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
public class EffectManager extends Manager<List<Effect>> {

    public void addEffect(Player player, EffectType type, long length) {
        addEffect(player, type, 1, length);
    }

    public void addEffect(Player player, EffectType type, int level, long length) {
       UtilServer.callEvent(new EffectReceiveEvent(player, new Effect(player.getUniqueId().toString(), type, level, length)));

    }

    public Optional<Effect> getEffect(Player player, EffectType type) {

        Optional<List<Effect>> effectsOptional = getObject(player.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            return effects.stream().filter(effect -> effect.getUuid().equalsIgnoreCase(player.getUniqueId().toString())
                    && effect.getEffectType() == type).findFirst();
        } else {
            return Optional.empty();
        }


    }

    public boolean hasEffect(Player player, EffectType type) {
        return getEffect(player, type).isPresent();
    }

    public void removeEffect(Player player, EffectType type) {
        Optional<List<Effect>> effectsOptional = getObject(player.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            effects.removeIf(effect -> effect.getUuid().equals(player.getUniqueId().toString()) && effect.getEffectType() == type);
        });
    }


    public void removeAllEffects(Player player){
        objects.remove(player.getUniqueId().toString());
    }

}
