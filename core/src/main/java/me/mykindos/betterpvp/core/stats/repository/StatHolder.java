package me.mykindos.betterpvp.core.stats.repository;

import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class StatHolder {

    protected abstract void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix);

}
