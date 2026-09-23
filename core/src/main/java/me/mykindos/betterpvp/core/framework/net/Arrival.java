package me.mykindos.betterpvp.core.framework.net;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Why a player was sent to another server: the instance they are for, and where in it they should land. */
@Value
public class Arrival {

    @NotNull RemoteInstance instance;
    /** A named landing the destination site resolves, instead of its usual arrival point. Null for the usual one. */
    @Nullable String landing;
}
