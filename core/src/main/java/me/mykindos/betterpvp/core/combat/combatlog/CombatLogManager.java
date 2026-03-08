package me.mykindos.betterpvp.core.combat.combatlog;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class CombatLogManager extends Manager<String, CombatLog> {

    public void createCombatLog(Player player, long expiry){
        if(player.isDead()) {
            return;
        }

        addObject(player.getUniqueId().toString(), new CombatLog(player, expiry));
    }

    public Optional<CombatLog> getCombatLogBySheep(LivingEntity target) {
        return getObjects().values().stream().filter(cl -> cl.getCombatLogSheep().getUniqueId().equals(target.getUniqueId())).findFirst();
    }
}
