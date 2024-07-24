package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ShockEffect extends EffectType {

    @Override
    public String getName() {
        return "Shock";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public boolean canStack() {
        return false;
    }

    @Override
    public void onTick(LivingEntity entity, Effect effect) {
        if(entity instanceof Player player) {
            player.playSound(net.kyori.adventure.sound.Sound.sound(Sound.ENTITY_PLAYER_HURT.key(),
                    net.kyori.adventure.sound.Sound.Source.PLAYER,
                    1f,
                    1f), player);
            player.playHurtAnimation(270);
        }
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> gives screen shake";
    }
}
