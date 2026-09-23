package me.mykindos.betterpvp.core.world.settler.wage;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** Where a site's wages are paid from. */
public interface CoinAccount {

    long balance(@NotNull SiteKey site);

    /** Takes {@code amount} out. Never asked for more than the balance. */
    void withdraw(@NotNull SiteKey site, long amount);
}
