package me.mykindos.betterpvp.core.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class Client {

    String uuid;
    String name;
    Rank rank;


    boolean administrating;

    public boolean hasRank(Rank rank){
        return this.rank.getId() >= rank.getId();
    }

}
