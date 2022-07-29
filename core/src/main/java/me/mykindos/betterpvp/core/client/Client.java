package me.mykindos.betterpvp.core.client;

import lombok.Value;

@Value
public class Client {

    String uuid;
    String name;
    Rank rank;

    public boolean hasRank(Rank rank){
        return this.rank.getId() >= rank.getId();
    }

}
