package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** A member hired a candidate into a camp, paying {@link #getPrice()} coins. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class SettlerHiredEvent extends CustomEvent {

    private final SiteKey site;
    private final Player player;
    private final Settler settler;
    private final long price;

    public SettlerHiredEvent(@NotNull SiteKey site, @NotNull Player player, @NotNull Settler settler, long price) {
        this.site = site;
        this.player = player;
        this.settler = settler;
        this.price = price;
    }
}
