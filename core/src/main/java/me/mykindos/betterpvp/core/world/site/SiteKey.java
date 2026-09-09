package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies whose instances of a site are being talked about. An owned site has one set of instances per owner, so
 * the owner forms part of the identity.
 */
@Value
public class SiteKey {

    /** The owner id used by every site that is not owned by anybody. */
    public static final long NO_OWNER = 0L;

    @NotNull String siteId;
    long ownerId;

    public static @NotNull SiteKey of(@NotNull String siteId) {
        return new SiteKey(siteId, NO_OWNER);
    }

    public static @NotNull SiteKey of(@NotNull String siteId, long ownerId) {
        return new SiteKey(siteId, ownerId);
    }

    public boolean isOwned() {
        return ownerId != NO_OWNER;
    }

    /** Round-trips through {@link #parse(String)}. */
    @Override
    public @NotNull String toString() {
        return isOwned() ? siteId + "#" + ownerId : siteId;
    }

    public static @NotNull SiteKey parse(@NotNull String stored) {
        final int separator = stored.indexOf('#');
        if (separator < 0) {
            return of(stored);
        }
        return of(stored.substring(0, separator), Long.parseLong(stored.substring(separator + 1)));
    }
}
