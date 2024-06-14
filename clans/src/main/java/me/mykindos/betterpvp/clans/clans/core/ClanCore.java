package me.mykindos.betterpvp.clans.clans.core;

import lombok.Data;
import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public final class ClanCore {

    private @NotNull Clan clan;
    private @Nullable Location position;
    private int energy;

    public ClanCore(@NotNull Clan clan) {
        this.clan = clan;
    }



}
