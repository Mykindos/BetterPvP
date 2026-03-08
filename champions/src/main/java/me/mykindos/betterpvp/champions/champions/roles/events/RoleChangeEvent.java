package me.mykindos.betterpvp.champions.champions.roles.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleChangeEvent extends CustomCancellableEvent {

    private final @NotNull LivingEntity livingEntity;
    private final @Nullable Role role;
    private final @Nullable Role previous;
}
