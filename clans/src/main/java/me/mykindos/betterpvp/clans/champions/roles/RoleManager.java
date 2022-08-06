package me.mykindos.betterpvp.clans.champions.roles;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.entity.Player;


@Singleton
public class RoleManager extends Manager<Role> {

    public RoleManager() {

    }

    public boolean hasRole(Player player, Role role){
        return objects.getOrDefault(player.getUniqueId().toString(), null) == role;
    }
}
