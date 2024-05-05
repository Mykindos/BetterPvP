package me.mykindos.betterpvp.shops.shops.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

@EqualsAndHashCode(callSuper = true)
@Data
public class ShopKeeperSpawnEvent extends CustomEvent {

    private final String shopkeeperType;
    private final Component name;
    private final Location location;
}
