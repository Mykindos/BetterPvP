package me.mykindos.betterpvp.core.world.site;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired once a site instance's world has closed and its folder has been left behind.
 * <p>
 * This is the last moment anything inside it can have changed, so it is where whatever keeps a record of an instance
 * writes that record down.
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public class SiteInstanceDormantEvent extends CustomEvent {

    private final @NotNull SiteInstance instance;

    public SiteInstanceDormantEvent(@NotNull SiteInstance instance) {
        this.instance = instance;
    }
}
