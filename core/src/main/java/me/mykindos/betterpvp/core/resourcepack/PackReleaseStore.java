package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.ImplementedBy;

import java.util.Optional;

/**
 * Where the pack server records the releases of each channel.
 */
@ImplementedBy(DatabasePackReleaseStore.class)
public interface PackReleaseStore {

    /**
     * The newest release of a channel, or empty when it has none.
     */
    Optional<PackRelease> latest(String channel);

}
