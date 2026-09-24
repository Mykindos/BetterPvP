package me.mykindos.betterpvp.clans.world.camp.settler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** A member paid {@link #getAmount()} of their own coins into a camp's wage fund. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class WageFundPaidEvent extends CustomEvent {

    private final SiteKey site;
    private final Player player;
    private final long amount;

    public WageFundPaidEvent(@NotNull SiteKey site, @NotNull Player player, long amount) {
        this.site = site;
        this.player = player;
        this.amount = amount;
    }
}
