package me.mykindos.betterpvp.clans.clans.components;

import lombok.Data;

@Data
public class ClanAlliance {

    private final String otherClan;
    private boolean trusted;

    public ClanAlliance(String otherClan, boolean trusted){
        this.otherClan = otherClan;
        this.trusted = trusted;
    }
}
