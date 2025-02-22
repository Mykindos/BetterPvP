package me.mykindos.betterpvp.core.chat.impl.chat;

import me.mykindos.betterpvp.core.chat.IFilterService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.concurrent.CompletableFuture;

public class DefaultFilterService implements IFilterService {

    @Override
    public CompletableFuture<Boolean> isFiltered(String message) {
        return CompletableFuture.supplyAsync(() -> false);
    }

    @Override
    public CompletableFuture<String> filterMessage(String message) {
        return CompletableFuture.supplyAsync(() -> message);
    }

    @Override
    public CompletableFuture<Component> filterMessage(Component message) {
        return CompletableFuture.supplyAsync(() -> {
            if (message instanceof TextComponent textComponent) {
                String plainText = textComponent.content();
                String filteredText = filterMessage(plainText).join();
                if (!plainText.equals(filteredText)) {
                    return Component.text(filteredText).style(textComponent.style())
                            .children(textComponent.children());
                }
            }
            return message;
        });
    }
}
