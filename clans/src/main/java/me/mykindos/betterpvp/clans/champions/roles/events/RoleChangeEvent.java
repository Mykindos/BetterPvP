package me.mykindos.betterpvp.clans.champions.roles.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleChangeEvent extends CustomEvent {

    private final Player player;
    private final Role role;
}
