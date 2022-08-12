package me.mykindos.betterpvp.core.utilities.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FetchNearbyEntityEvent<T extends LivingEntity> extends CustomEvent {

    private final LivingEntity source;
    private final Location location;
    private final List<KeyValue<T, EntityProperty>> entities;
    private final EntityProperty entityProperty;

}
