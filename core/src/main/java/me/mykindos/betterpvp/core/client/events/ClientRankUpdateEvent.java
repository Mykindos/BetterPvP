package me.mykindos.betterpvp.core.client.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ClientRankUpdateEvent extends CustomEvent {

    private final Client client;
    private final Rank oldRank;
    private final Rank newRank;

    public ClientRankUpdateEvent(Client client, Rank oldRank, Rank newRank) {
        this.client = client;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }
}
