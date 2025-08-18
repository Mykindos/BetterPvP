package me.mykindos.betterpvp.core.chat.filter.impl;

import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.chat.ChatModule;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MineplexFilterService extends DefaultFilterService {

    private final ChatModule chatModule;

    public MineplexFilterService() {
        this.chatModule = MineplexModuleManager.getRegisteredModule(ChatModule.class);
    }

    @Override
    public CompletableFuture<Boolean> isFiltered(String message) {
        return CompletableFuture.supplyAsync(() -> chatModule.isFiltered(message));
    }

    @Override
    public CompletableFuture<String> filterMessage(String message) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> filteredTextOptional = chatModule.getFilteredMessage(message);
            return filteredTextOptional.orElse(message);
        });
    }

}
