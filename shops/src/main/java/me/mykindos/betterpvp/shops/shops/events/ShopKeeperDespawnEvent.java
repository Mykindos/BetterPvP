package me.mykindos.betterpvp.shops.shops.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
public class ShopKeeperDespawnEvent extends CustomEvent {

    private final Entity entity;
}
