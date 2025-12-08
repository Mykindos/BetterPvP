package me.mykindos.betterpvp.core.supplycrate.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrate;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class SupplyCrateLandEvent extends CustomEvent {

    private final Player caller;
    private final SupplyCrate supplyCrate;

}
