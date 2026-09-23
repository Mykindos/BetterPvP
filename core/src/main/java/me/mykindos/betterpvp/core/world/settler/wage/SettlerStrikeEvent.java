package me.mykindos.betterpvp.core.world.settler.wage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** A site's paid settlers went on strike because their wages ran out, or went back to work once paid. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class SettlerStrikeEvent extends CustomEvent {

    private final SiteKey site;
    private final List<Settler> settlers;
    /** True when they stopped work, false when they went back to it. */
    private final boolean striking;

    public SettlerStrikeEvent(@NotNull SiteKey site, @NotNull List<Settler> settlers, boolean striking) {
        this.site = site;
        this.settlers = List.copyOf(settlers);
        this.striking = striking;
    }
}
