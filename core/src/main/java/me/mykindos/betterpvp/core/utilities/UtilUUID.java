package me.mykindos.betterpvp.core.utilities;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UtilUUID {
    @Nullable
    public static UUID fromString(@Nullable String id) {
        if (id == null) {
            return null;
        }
        return UUID.fromString(id);
    }
}
