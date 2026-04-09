package me.mykindos.betterpvp.core.framework.server.network;

import java.util.Map;

public interface NetworkPlayerCountService {

    /**
     * Returns a count of all online players in the <b>network</b>
     */
    int getOnlineCount();

    /**
     * Returns a snapshot of all server names to player counts known to the proxy.
     * Server names are lowercase (e.g. {@code "clans-1"}).
     */
    Map<String, Integer> getServerPlayerCounts();

}
