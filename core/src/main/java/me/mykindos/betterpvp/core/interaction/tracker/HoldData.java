package me.mykindos.betterpvp.core.interaction.tracker;

import lombok.Value;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.LivingEntity;

/**
 * Tracks data about currently held items for HOLD input triggers.
 */
@Value
public class HoldData {
    LivingEntity entity;
    ItemInstance instance;
}
