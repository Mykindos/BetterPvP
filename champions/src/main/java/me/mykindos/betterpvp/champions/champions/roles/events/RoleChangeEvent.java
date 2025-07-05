package me.mykindos.betterpvp.champions.champions.roles.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleChangeEvent extends CustomEvent {

    private final Player player;
    @Nullable
    private final Role role;
    @Nullable
    private final Role previous;
}
