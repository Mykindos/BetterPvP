package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** New candidates are waiting at a camp's Dock, brought by a boat or sent for a clan milestone. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class SettlerBoatEvent extends CustomEvent {

    private final SiteKey site;
    /** True when they were sent for a clan milestone rather than brought by a boat. */
    private final boolean milestone;

    public SettlerBoatEvent(@NotNull SiteKey site, boolean milestone) {
        this.site = site;
        this.milestone = milestone;
    }
}
