package me.mykindos.betterpvp.core.server;

import lombok.Data;

@Data
public class Realm implements Period {
    private final int id;
    private final Server server;
    private final Season season;

}
