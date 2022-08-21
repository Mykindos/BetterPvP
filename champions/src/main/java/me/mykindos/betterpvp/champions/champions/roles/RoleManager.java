package me.mykindos.betterpvp.champions.champions.roles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.entity.Player;


@Singleton
public class RoleManager extends Manager<Role> {

    @Getter
    private final RoleRepository repository;

    @Inject
    public RoleManager(RoleRepository repository) {
        this.repository = repository;
    }

    /**
     * Check if a player has a specific role equipped
     * @param player The player
     * @param role The role
     * @return True if the player has the target role equipped
     */
    public boolean hasRole(Player player, Role role){
        return objects.getOrDefault(player.getUniqueId().toString(), null) == role;
    }

    /**
     * Check if a player has any role equipped
     * @param player The player
     * @return True if the player has a role
     */
    public boolean hasRole(Player player) {
        return objects.containsKey(player.getUniqueId().toString());
    }
}
