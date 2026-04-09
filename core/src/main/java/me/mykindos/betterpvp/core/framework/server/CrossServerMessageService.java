package me.mykindos.betterpvp.core.framework.server;

import java.util.concurrent.CompletableFuture;

public interface CrossServerMessageService {

    void broadcast(ServerMessage message);

    CompletableFuture<Boolean> isPlayerOnline(String playerName);
}
