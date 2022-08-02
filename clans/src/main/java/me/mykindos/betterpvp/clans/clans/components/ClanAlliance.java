package me.mykindos.betterpvp.clans.clans.components;

import lombok.Data;
import me.mykindos.betterpvp.clans.clans.Clan;

@Data
public class ClanAlliance {

    private final Clan clan;
    private boolean trusted;

    public ClanAlliance(Clan clan, boolean trusted){
        this.clan = clan;
        this.trusted = trusted;
    }
}
