package me.mykindos.betterpvp.core.chat.filter.impl;

import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DefaultFilterService implements IFilterService {

    @Override
    public CompletableFuture<Boolean> isFiltered(String message) {
        return CompletableFuture.supplyAsync(() -> false);
    }

    @Override
    public CompletableFuture<Boolean> addFilteredWord(String word) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> removeFilteredWord(String word) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<String> filterMessage(String message) {
        return CompletableFuture.supplyAsync(() -> message);
    }

    @Override
    public CompletableFuture<Component> filterMessage(Component message) {
        if (message instanceof TextComponent textComponent) {
            String plainText = textComponent.content();
            return filterMessage(plainText).thenApply(filteredText -> {
                if (!plainText.equals(filteredText)) {
                    return Component.text(filteredText).style(textComponent.style())
                            .children(textComponent.children());
                }
                return message;
            });
        }
        return CompletableFuture.completedFuture(message);
    }

    @Override
    public Set<String> getFilteredWords() {
        return Set.of();
    }
}
