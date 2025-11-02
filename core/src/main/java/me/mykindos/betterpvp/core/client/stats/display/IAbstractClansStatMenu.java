package me.mykindos.betterpvp.core.client.stats.display;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IAbstractClansStatMenu extends IAbstractStatMenu {
    String getClanName();
    @Nullable
    UUID getClanID();
}
