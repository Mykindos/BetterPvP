package me.mykindos.betterpvp.core.world.settler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A settler was given a workplace, moved to another, or taken off one. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class SettlerAssignedEvent extends CustomEvent {

    private final SiteKey site;
    private final Settler settler;
    private final @Nullable String previous;

    public SettlerAssignedEvent(@NotNull SiteKey site, @NotNull Settler settler, @Nullable String previous) {
        this.site = site;
        this.settler = settler;
        this.previous = previous;
    }
}
