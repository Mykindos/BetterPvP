package me.mykindos.betterpvp.champions.champions.roles.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleChangeEvent extends CustomCancellableEvent {

    private final Player player;
    private final Role role;
    private final Role previous;
}
