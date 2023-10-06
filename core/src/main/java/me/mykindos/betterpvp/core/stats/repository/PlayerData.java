package me.mykindos.betterpvp.core.stats.repository;

import me.mykindos.betterpvp.core.database.Database;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class PlayerData {

    protected abstract void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix);

    public abstract Component[] getDescription();

}
