package me.mykindos.betterpvp.core.world.settler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** A settler joined a site's roster. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class SettlerJoinedEvent extends CustomEvent {

    private final SiteKey site;
    private final Settler settler;

    public SettlerJoinedEvent(@NotNull SiteKey site, @NotNull Settler settler) {
        this.site = site;
        this.settler = settler;
    }
}
