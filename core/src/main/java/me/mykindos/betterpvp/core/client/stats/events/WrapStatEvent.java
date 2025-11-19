package me.mykindos.betterpvp.core.client.stats.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

import java.util.UUID;


/**
 * An event that allows an IStat to be wrapped if necessary to provide more info
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class WrapStatEvent extends CustomEvent {
    /**
     * The ID of the player
     */
    UUID id;
    /**
     * The stat to potentially wrap
     */
    IStat stat;
}
