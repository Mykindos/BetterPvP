package me.mykindos.betterpvp.core.chat.filter;

import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

public interface IFilterService {

    CompletableFuture<Boolean> isFiltered(String message);

    CompletableFuture<String> filterMessage(String message);

    CompletableFuture<Component> filterMessage(Component message);
}
