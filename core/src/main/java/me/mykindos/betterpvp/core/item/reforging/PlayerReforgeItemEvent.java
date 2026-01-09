package me.mykindos.betterpvp.core.item.reforging;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatAugmentationComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Value
public class PlayerReforgeItemEvent extends CustomCancellableEvent {

    Player player;
    ItemInstance itemInstance;
    @Nullable StatAugmentationComponent statAugmentationComponent;

}
