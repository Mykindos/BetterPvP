package me.mykindos.betterpvp.core.supplycrate.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateType;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class SupplyCrateDeployEvent extends CustomCancellableEvent {

    private final Player caller;
    private final SupplyCrateType type;

}
