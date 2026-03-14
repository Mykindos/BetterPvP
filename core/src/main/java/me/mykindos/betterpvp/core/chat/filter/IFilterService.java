package me.mykindos.betterpvp.core.chat.filter;

import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface IFilterService {

    CompletableFuture<Boolean> isFiltered(String message);
    
    CompletableFuture<Boolean> addFilteredWord(String word);
    
    CompletableFuture<Boolean> removeFilteredWord(String word);

    CompletableFuture<String> filterMessage(String message);

    CompletableFuture<Component> filterMessage(Component message);

    Set<String> getFilteredWords();
}
