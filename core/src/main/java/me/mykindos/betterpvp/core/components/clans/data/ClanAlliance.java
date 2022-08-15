package me.mykindos.betterpvp.core.components.clans.data;

import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IClan;

@Data
public class ClanAlliance {

    private final IClan clan;
    private boolean trusted;

    public ClanAlliance(IClan clan, boolean trusted){
        this.clan = clan;
        this.trusted = trusted;
    }
}
