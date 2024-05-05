package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class FrozenEffect extends EffectType {

    HashMap<UUID, GameMode> previousGamemode = new HashMap<>();

    @Override
    public String getName() {
        return "Frozen";
    }

    @Override
    public String getDescription(int level) {
        return "Freezes the player, preventing movement, damage, and other actions";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        super.onReceive(livingEntity, effect);
        if (livingEntity instanceof Player player) {
            previousGamemode.putIfAbsent(player.getUniqueId(), player.getGameMode());
            player.setGameMode(GameMode.ADVENTURE);
            UtilMessage.message(player, "Frozen", "You have been frozen");
        }
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect) {
        super.onExpire(livingEntity, effect);
        if (livingEntity instanceof Player player) {
            player.setGameMode(previousGamemode.remove(player.getUniqueId()));
            UtilMessage.message(player, "Frozen", "You are unfrozen");
        }
    }
}
