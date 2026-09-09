package me.mykindos.betterpvp.core.world.site;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

/**
 * Fired just before a site's world folder is deleted, so anything holding state keyed to that world can drop it.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SiteWorldReleasedEvent extends CustomEvent {

    private final String world;

}
