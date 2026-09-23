package me.mykindos.betterpvp.core.world.settler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** A settler left a site's roster for good. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class SettlerLeftEvent extends CustomEvent {

    private final SiteKey site;
    private final Settler settler;
    private final SettlerLeaveReason reason;

    public SettlerLeftEvent(@NotNull SiteKey site, @NotNull Settler settler, @NotNull SettlerLeaveReason reason) {
        this.site = site;
        this.settler = settler;
        this.reason = reason;
    }
}
