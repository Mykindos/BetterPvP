package me.mykindos.betterpvp.core.resourcepack;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Called on the main thread when this server's channel starts serving a different release.
 */
@Getter
public class PackReleaseChangedEvent extends CustomEvent {

    private final @Nullable PackRelease previous;
    private final PackRelease current;

    public PackReleaseChangedEvent(@Nullable PackRelease previous, PackRelease current) {
        this.previous = previous;
        this.current = current;
    }

}
